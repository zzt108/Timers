Feature: Timer Operations
  As a user
  I want to control my timers
  So that I can manage my time effectively

  Background:
    Given I am on the main screen
    And I create a timer named "Study" with duration 25 minutes

  @wip
  Scenario: Start and Pause a timer
    When I tap the start button for "Study"
    Then the timer "Study" should be running
    When I tap the pause button for "Study"
    Then the timer "Study" should be paused

  @wip
  Scenario: Reset a timer
    When I tap the start button for "Study"
    And I wait for 2 seconds
    And I tap the reset button for "Study"
    Then the timer "Study" should show "25 min"

  @wip
  Scenario: Delete a timer
    When I tap the delete button for "Study"
    Then I should not see a timer named "Study"

  @wip
  Scenario: Edit a timer
    When I tap the edit button for "Study"
    And I set the timer duration to 30 minutes
    And I tap the update button
    Then I should see a timer named "Study" with duration "30 min"
