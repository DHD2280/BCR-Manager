package it.dhd.bcrmanager.json;

public class Call {
    private String phone_number;
    private String phone_number_formatted;

    // Constructors, getters, and setters

    public String getPhoneNumber() {
        return phone_number;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phone_number = phoneNumber;
    }

    public String getPhoneNumberFormatted() {
        return phone_number_formatted;
    }

    public void setPhoneNumberFormatted(String phoneNumberFormatted) {
        this.phone_number_formatted = phoneNumberFormatted;
    }
}
