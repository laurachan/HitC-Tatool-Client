/**
 * Copyright 2012 Laura Chan
 * This file is part of Heads in the Cloud Tatool Client.
 * 
 * Heads in the Cloud Tatool Client is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Heads in the Cloud Tatool Client is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Heads in the Cloud Tatool Client.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.uvic.hitc.executable;

import java.awt.event.KeyEvent;
import java.util.Random;

import ch.tatool.core.data.*;
import ch.tatool.core.display.swing.*;
import ch.tatool.core.display.swing.action.*;
import ch.tatool.core.display.swing.container.*;
import ch.tatool.core.display.swing.container.RegionsContainer.Region;
import ch.tatool.core.display.swing.panel.CenteredTextPanel;
import ch.tatool.core.executable.BlockingAWTExecutable;
import ch.tatool.data.*;

public class EchoExecutable extends BlockingAWTExecutable implements ActionPanelListener, DescriptivePropertyHolder {

	CenteredTextPanel questionPanel;
	KeyActionPanel actionPanel;
	Random random;
	
	Trial currentTrial;
	int correctAnswer;
	int response;
	
	public EchoExecutable() {
		questionPanel = new CenteredTextPanel();
		actionPanel = new KeyActionPanel();
		actionPanel.addActionPanelListener(this);
		random = new Random();
	}
	
	/**
	 * Execution begins here.
	 */
	@Override
	protected void startExecutionAWT() {
		// Boilerplate for displaying the question: using a RegionsContainer allows us to customize the UI layout
		SwingExecutionDisplay display = ExecutionDisplayUtils.getDisplay(this.getExecutionContext());
		ContainerUtils.showRegionsContainer(display);
		RegionsContainer container = ContainerUtils.getRegionsContainer(); 
		
		currentTrial = this.getExecutionContext().getExecutionData().addTrial();
		currentTrial.setParentId(this.getId());
		
		correctAnswer = random.nextInt(10);
		
		questionPanel.setText("" + correctAnswer);
		container.setRegionContent(Region.CENTER, questionPanel);
		container.setRegionContentVisibility(Region.CENTER, true);
		CenteredTextPanel instructionPanel = new CenteredTextPanel();
		instructionPanel.setText("Type the number shown above.");
		container.setRegionContent(Region.SOUTH, instructionPanel);
		container.setRegionContentVisibility(Region.SOUTH, true);
		
		actionPanel.addKey(KeyEvent.VK_0, "0", 0);
		actionPanel.addKey(KeyEvent.VK_1, "1", 1);
		actionPanel.addKey(KeyEvent.VK_2, "2", 2);
		actionPanel.addKey(KeyEvent.VK_3, "3", 3);
		actionPanel.addKey(KeyEvent.VK_4, "4", 4);
		actionPanel.addKey(KeyEvent.VK_5, "5", 5);
		actionPanel.addKey(KeyEvent.VK_6, "6", 6);
		actionPanel.addKey(KeyEvent.VK_7, "7", 7);
		actionPanel.addKey(KeyEvent.VK_8, "8", 8);
		actionPanel.addKey(KeyEvent.VK_9, "9", 9);
		actionPanel.enableActionPanel();
	}

	public void actionTriggered(ActionPanel source, Object actionValue) {
		actionPanel.disableActionPanel();
		response = (Integer) actionValue;
		stop();
	}

	private void stop() {
		// This stuff has to do with recording trial data
		Question.getQuestionProperty().setValue(this, "" + correctAnswer);
		Question.getAnswerProperty().setValue(this, "" + correctAnswer);
		Question.getResponseProperty().setValue(this, response);
		boolean correct = response == correctAnswer;
		Points.setZeroOnePoints(this, correct);
		Result.getResultProperty().setValue(this, correct);
		DataUtils.storeProperties(currentTrial, this);
		
		// Don't just try to go straight to container.removeAllContent(), or next time
		// the question text won't appear!
		RegionsContainer container = ContainerUtils.getRegionsContainer();
		container.setRegionContentVisibility(Region.CENTER, false);
		container.setRegionContentVisibility(Region.SOUTH, false);
		container.removeRegionContent(Region.CENTER);
		container.removeRegionContent(Region.SOUTH);
		
		if (this.getFinishExecutionLock()) {
			this.finishExecution();
		}
	}

	public Property<?>[] getPropertyObjects() {
		return new Property[] {
				Question.getQuestionProperty(),
				Question.getAnswerProperty(),
				Question.getResponseProperty(),
				Result.getResultProperty(),
				Points.getPointsProperty()
		};
	}
}
