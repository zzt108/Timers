Feature: Timer Creation
  As a user
  I want to create a new timer
  So that I can time my activities

  @wip
  Scenario: User creates a timer with default name
    Given I am on the main screen
    And I tap the add timer button
    When I set the timer duration to 5 minutes
    And I tap the add button
    Then I should see a timer named "Timer 1" with duration "5 min"

  @wip
  Scenario: User creates a timer with custom name
    Given I am on the main screen
    And I tap the add timer button
    When I set the timer duration to 10 minutes
    And I enter "Egg Timer" as the timer name
    And I tap the add button
    Then I should see a timer named "Egg Timer" with duration "10 min"

  @wip
  Scenario: User attempts to create a zero duration timer
    Given I am on the main screen
    And I tap the add timer button
    When I set the timer duration to 0 minutes
    And I tap the add button
    Then I should see an error message "Timer duration must be greater than zero"
