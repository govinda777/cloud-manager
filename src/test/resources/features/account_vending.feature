Feature: Account Provisioning and Recipe Deployment
  As a Cloud Platform Engineer
  I want to automatically provision a new cloud account on feature PRs
  So that I can validate that the account recipe applies correctly without drift or security violations

  @aws @sandbox
  Scenario: Provision a clean AWS Sandbox Account for a Feature PR
    Given I have authenticated to AWS via OIDC as the "CloudManagerPipelineRole"
    When I request a new AWS Account for PR "PR-42" under the "Sandbox-OU"
    Then the account creation process should succeed within 5 minutes
    And the new account should have the mandatory SCPs applied

  @recipe @integration @sandbox
  Scenario: Apply Cloud Manager Infrastructure Recipe
    Given a clean target AWS Account is provisioned for "PR-42"
    When I apply the Cloud Manager terraform recipe to the account
    Then all core resources should be successfully created:
      | Resource           | Type                     | Expected State |
      | Audit Trail Bucket | aws_s3_bucket            | Encrypted      |
      | Default VPC        | aws_vpc                  | Flow Logs On   |
      | IAM Admin Role     | aws_iam_role             | MFA Enforced   |
    And no manual interventions should be required during the deployment

  @teardown @sandbox
  Scenario: Teardown and Cleanup Resources
    Given the BDD test execution for "PR-42" is finished
    When I trigger the teardown process for the PR environment
    Then all created cloud resources should be destroyed
    And the AWS Account should be closed or moved to the "Quarantine-OU"
