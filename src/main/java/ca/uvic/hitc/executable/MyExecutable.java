/*******************************************************************************
 * Copyright (c) 2011 Michael Ruflin, André Locher, Claudia von Bastian.
 * 
 * This file is part of Heads in the Cloud Tatool Client.
 * 
 * Heads in the Cloud Tatool Client is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published 
 * by the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * Heads in the Cloud Tatool Client is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Heads in the Cloud Tatool Client. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package ca.uvic.hitc.executable;

import java.awt.event.KeyEvent;
import java.util.Date;
import java.util.Random;

import ch.tatool.core.data.DataUtils;
import ch.tatool.core.data.Misc;
import ch.tatool.core.data.Points;
import ch.tatool.core.data.Question;
import ch.tatool.core.data.Result;
import ch.tatool.core.data.Timing;
import ch.tatool.core.display.swing.ExecutionDisplayUtils;
import ch.tatool.core.display.swing.SwingExecutionDisplay;
import ch.tatool.core.display.swing.action.ActionPanel;
import ch.tatool.core.display.swing.action.ActionPanelListener;
import ch.tatool.core.display.swing.action.KeyActionPanel;
import ch.tatool.core.display.swing.container.ContainerUtils;
import ch.tatool.core.display.swing.container.RegionsContainer;
import ch.tatool.core.display.swing.container.RegionsContainer.Region;
import ch.tatool.core.display.swing.panel.CenteredTextPanel;
import ch.tatool.core.executable.BlockingAWTExecutable;
import ch.tatool.data.DescriptivePropertyHolder;
import ch.tatool.data.Property;
import ch.tatool.data.Trial;
import ch.tatool.exec.ExecutionContext;
import ch.tatool.exec.ExecutionOutcome;

public class MyExecutable extends BlockingAWTExecutable implements
		ActionPanelListener, DescriptivePropertyHolder {

	private CenteredTextPanel questionPanel;
	private KeyActionPanel actionPanel;

	private Random rand;
	
	private Trial currentTrial;
	private Response correctResponse;
	private Response givenResponse;
	private long startTime;
	private long endTime;

	private enum Response {
		GREEN, BLUE, NONE
	}

	public MyExecutable() {
		// setup user interface components
		questionPanel = new CenteredTextPanel();
		actionPanel = new KeyActionPanel();
		actionPanel.addActionPanelListener(this);

		// create a new random object
		rand = new Random();
	}

	protected void startExecutionAWT() {
		// get the execution display with the help of the execution context
		ExecutionContext context = getExecutionContext();
		SwingExecutionDisplay display = ExecutionDisplayUtils.getDisplay(context);

		// get the execution display to use the regionsContainer
		ContainerUtils.showRegionsContainer(display);

		// get the regions container we can use to add components to the UI
		RegionsContainer regionsContainer = ContainerUtils.getRegionsContainer();

		// init data
		String output = initData();

		// setup the views
		questionPanel.setTextSize(120);
		questionPanel.setText(output);
		actionPanel.addKey(KeyEvent.VK_A, "Green", Response.GREEN);
		actionPanel.addKey(KeyEvent.VK_L, "Blue", Response.BLUE);
		regionsContainer.setRegionContent(Region.CENTER, questionPanel);
		regionsContainer.setRegionContent(Region.SOUTH, actionPanel);
		
		// create new Trial object
		currentTrial = getExecutionContext().getExecutionData().addTrial();
		currentTrial.setParentId(getId());
		
		// set start time property
		Timing.getStartTimeProperty().setValue(this, new Date());
		startTime = System.nanoTime();

		// display regions
		regionsContainer.setRegionContentVisibility(Region.CENTER, true);
		regionsContainer.setRegionContentVisibility(Region.SOUTH, true);

		// enable the actions
		actionPanel.enableActionPanel();
	}

	private String initData() {
		// create two random numbers
		int randomWord = rand.nextInt(2);
		int randomColor = rand.nextInt(2);

		// randomly pick blue or green as the word to display
		String word = "";
		switch (randomWord) {
		case 0:
			word = "green";
			break;
		case 1:
			word = "blue";
			break;
		default:
			break;
		}

		// randomly pick blue or green as the font color
		String output = "";
		switch (randomColor) {
		case 0:
			output = "<html><font color='#009933'>" + word + "</font></html>";
			correctResponse = Response.GREEN;
			break;
		case 1:
			output = "<html><font color='#1e78b4'>" + word + "</font></html>";
			correctResponse = Response.BLUE;
			break;
		default:
			break;
		}

		// set stimulus properties
		Question.setQuestionAnswer(this, word, correctResponse);
		Points.setZeroOneMinMaxPoints(this);
		givenResponse = Response.NONE;
		return output;
	}
	
	public void actionTriggered(ActionPanel source, Object actionValue) {
		// disable action panel
		actionPanel.disableActionPanel();
		
		// set end time property
		endTime = System.nanoTime();
		Timing.getEndTimeProperty().setValue(this, new Date());

		// get the users' response
		givenResponse = (Response) actionValue;

		// store the data and stop the execution
		endTask();
	}
	
	private void endTask() {
		// remove content from display
		RegionsContainer regionsContainer = ContainerUtils.getRegionsContainer();
		regionsContainer.setRegionContentVisibility(Region.CENTER, false);
		regionsContainer.setRegionContentVisibility(Region.SOUTH, false);
		regionsContainer.removeRegionContent(Region.CENTER);
		regionsContainer.removeRegionContent(Region.SOUTH);

		// set more stimulus properties
		Question.getResponseProperty().setValue(this, givenResponse);
		boolean success = correctResponse.equals(givenResponse);
		Points.setZeroOnePoints(this, success);
		Result.getResultProperty().setValue(this, success);
		Misc.getOutcomeProperty().setValue(this, ExecutionOutcome.FINISHED);

		// set duration time property
		long duration = 0;
		if (endTime > 0) {
			duration = endTime - startTime;
		}
		long ms = (long) duration / 1000000;
		Timing.getDurationTimeProperty().setValue(this, ms);

		// add all executable properties to our trial object
		DataUtils.storeProperties(currentTrial, this);

		// finish the execution and make sure nobody else already did so
		if (getFinishExecutionLock()) {
			finishExecution();
		}
	}

	public Property<?>[] getPropertyObjects() {
		return new Property[] { Points.getMinPointsProperty(),
				Points.getPointsProperty(), Points.getMaxPointsProperty(),
				Question.getQuestionProperty(), Question.getAnswerProperty(),
				Question.getResponseProperty(), Result.getResultProperty(),
				Timing.getStartTimeProperty(), Timing.getEndTimeProperty(),
				Timing.getDurationTimeProperty(), Misc.getOutcomeProperty() };
	}

}
