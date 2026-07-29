Feature: Dashboard and Error Propagation for UI

  Scenario: Dashboard updates total counts correctly
    Given the database has seed accounts for "AWS"
    When a new "AWS" account named "Dashboard-Test-AWS" with email "dash@aws.com" and cost center "CC-DASHBOARD" is created
    Then the dashboard should show 2 total accounts
    And the dashboard active accounts should be 2

  Scenario: Error message is propagated for failed accounts
    Given the database has seed accounts for "GCP"
    When a new "GCP" account named "fail-me" with email "failed@gcp.com" and cost center "CC-FAIL" is created
    Then the account state should be "FAILED"
    And the account detail should have error message "Simulated cloud provisioning failure"
