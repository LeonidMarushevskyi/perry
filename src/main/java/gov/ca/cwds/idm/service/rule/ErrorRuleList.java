package gov.ca.cwds.idm.service.rule;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ErrorRuleList {

  private List<ErrorRule> rules = new LinkedList<>();

  public ErrorRuleList add(ErrorRule rule) {
    rules.add(rule);
    return this;
  }

  public ErrorRuleList addAll(Collection<ErrorRule> rules) {
    rules.addAll(rules);
    return this;
  }

  public void check() {
    for (ErrorRule rule : rules) {
      rule.check();
    }
  }

  public boolean hasError() {
    for (ErrorRule rule : rules) {
      if (rule.hasError()) {
        return true;
      }
    }
    return false;
  }
}
