Feature: Provisioning Cloud Accounts

  Scenario: Successfully provisioning an AWS account with budget inheritance
    Given a valid request to create a new "AWS" account named "Marketing-Prod" with email "marketing-prod@gcp.com" and cost center "CC-MARKETING"
    When the account creation request is submitted
    Then the account state should be updated to "ACTIVE"
    And the account should be linked to the "AWS" seed account
