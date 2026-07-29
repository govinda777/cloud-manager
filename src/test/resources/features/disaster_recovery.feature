Feature: Platform Disaster Recovery

  Scenario: Account provisioning fails when the Seed Account is missing (Disaster State)
    Given the "AWS" master seed account is deleted from the platform database
    When a request to create a new "AWS" account named "Sales-Prod" is submitted
    Then the account state of the new request should be updated to "FAILED"
    And the error message of the new request should indicate that the seed account is missing

  Scenario: Recovery of the Platform by recreating the Master Seed Account
    Given a new "AWS" master seed account is registered in the database
    When a request to create a new "AWS" account named "Sales-Prod-Recovered" is submitted
    Then the account state of the new request should be updated to "ACTIVE"
    And the new request should be linked to the new "AWS" seed account
