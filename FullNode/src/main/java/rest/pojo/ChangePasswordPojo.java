package rest.pojo;

public class ChangePasswordPojo {
    private char[] prevPassword; // could be null if root requests for changing
    private char[] newPassword;

    public char[] getPrevPassword() {
        return prevPassword;
    }

    public void setPrevPassword(char[] prevPassword) {
        this.prevPassword = prevPassword;
    }

    public char[] getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(char[] newPassword) {
        this.newPassword = newPassword;
    }

}
