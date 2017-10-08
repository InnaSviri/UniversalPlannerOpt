package ru.programpark.entity;

/**
 * User: oracle
 * Date: 10.11.14
 */
public class Plan {
    private String ReasonForNotBeingAssigned;//причина почему не удалось подвязать этот план
    public enum AssignedDetails {NOT_ASSIGNED, ASSIGNED_MANUALLY, ASSIGNED_RECURSIVELY};
    private AssignedDetails details = AssignedDetails.NOT_ASSIGNED;
    private String comment = "";
    private Long id;

    public Plan(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReasonForNotBeingAssigned() {
        return ReasonForNotBeingAssigned;
    }

    public void setReasonForNotBeingAssigned(String reasonForNotBeingAssigned) {
        ReasonForNotBeingAssigned = reasonForNotBeingAssigned;
    }

    public AssignedDetails getDetails() {
        return details;
    }

    public void setDetails(AssignedDetails details) {
        this.details = details;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
