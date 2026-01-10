package org.example.shared;


public class Protocol {

    public static final String CMD_LOGIN = "LOGIN";
    public static final String CMD_ADD_REPORT = "ADD_REPORT";
    public static final String CMD_EDIT_REPORT = "EDIT_REPORT";
    public static final String CMD_LIST_REPORTS = "LIST_REPORTS";
    public static final String CMD_LIST_PATIENTS = "LIST_PATIENTS";
    public static final String CMD_LOGOUT = "LOGOUT";


    public static final String RESP_OK = "OK";
    public static final String RESP_ERROR = "ERROR";
    public static final String RESP_SALT = "SALT";
    public static final String RESP_DIGEST = "DIGEST";


    public static final String DELIMITER = "|";


    public static final String SYMMETRIC_ALGORITHM = "AES";
    public static final String ASYMMETRIC_ALGORITHM = "RSA";
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final String HMAC_ALGORITHM = "HmacSHA256";


    public static final int AES_KEY_SIZE = 256;
    public static final int RSA_KEY_SIZE = 2048;
    public static final int SALT_SIZE = 16;
}
