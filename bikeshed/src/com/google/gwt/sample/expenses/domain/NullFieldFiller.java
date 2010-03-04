package com.google.gwt.sample.expenses.domain;

/**
 * Does the merging that a persistence framework would have done for us. If
 * persistence frameworks did this "null field means no change" kind of thing.
 * Which seems unlikely. But it was fun to write. D'oh.
 * <p>
 * Class cast exceptions thrown on merge type mismatch.
 */
final class NullFieldFiller implements EntityVisitor<Void> {
  private final Entity sparseEntity;

  /**
   * @param sparseEntity any null fields on this object will be filled from the
   *          fields of the Entity that accepts this NullFieldFiller
   */
  NullFieldFiller(Entity sparseEntity) {
    this.sparseEntity = sparseEntity;
  }

  public Void visit(Currency currency) {
    Currency sparse = ((Currency) sparseEntity);
    if (null == sparse.getCode()) {
      sparse.setCode(currency.getCode());
    }
    if (null == sparse.getName()) {
      sparse.setName(currency.getName());
    }
    return null;
  }

  public Void visit(Employee employee) {
    Employee sparse = ((Employee) sparseEntity);
    if (null == sparse.getUserName()) {
      sparse.setUserName(employee.getUserName());
    }
    if (null == sparse.getSupervisor()) {
      sparse.setSupervisor(employee.getSupervisor());
    }
    if (null == sparse.getDisplayName()) {
      sparse.setDisplayName(employee.getDisplayName());
    }
    return null;
  }

  public Void visit(Report report) {
    Report sparse = ((Report) sparseEntity);
    if (sparse.getApproved_supervisor() == null) {
      sparse.setApproved_supervisor(report.getApproved_supervisor());
    }
    if (sparse.getPurpose() == null) {
      sparse.setPurpose(report.getPurpose());
    }
    if (sparse.getReporter() == null) {
      sparse.setReporter(report.getReporter());
    }
    if (sparse.getStatus() == null) {
      sparse.setStatus(report.getStatus());
    }
    return null;
  }

  public Void visit(ReportItem reportItem) {
    ReportItem sparse = ((ReportItem) sparseEntity);
    if (null == sparse.getAmount()) {
      sparse.setAmount(reportItem.getAmount());
    }
    if (null == sparse.getCurrency()) {
      sparse.setCurrency(reportItem.getCurrency());
    }
    if (null == sparse.getIncurred()) {
      sparse.setIncurred(reportItem.getIncurred());
    }
    if (null == sparse.getPurpose()) {
      sparse.setPurpose(reportItem.getPurpose());
    }
    if (null == sparse.getReport()) {
      sparse.setReport(reportItem.getReport());
    }
    return null;
  }
}