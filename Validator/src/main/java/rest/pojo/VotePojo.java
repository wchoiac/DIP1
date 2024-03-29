package rest.pojo;

public class VotePojo {
    private AuthorityInfoPojo beneficiary;
    private boolean add; // true:add, false:remove
    private boolean agree;

    public AuthorityInfoPojo getBeneficiary() {
        return beneficiary;
    }

    public void setBeneficiary(AuthorityInfoPojo beneficiary) {
        this.beneficiary = beneficiary;
    }

    public boolean isAdd() {
        return add;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }

    public boolean isAgree() {
        return agree;
    }

    public void setAgree(boolean agree) {
        this.agree = agree;
    }

}
