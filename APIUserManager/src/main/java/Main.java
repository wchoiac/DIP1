import config.Configuration;
import manager.UserInfoManager;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        Console console = System.console();
        Scanner sc = new Scanner(System.in);


        int selection=0;
        while(selection<3&&selection>=0) {


            System.out.println("0. Register user");
            System.out.println("1. Register root user (only for validator)");
            System.out.println("2. Change user password");
            System.out.println("3. Quit");

            System.out.println("Choose your action:");
            selection = sc.nextInt();

            switch (selection) {

                case 0:
                    makeUser(sc,console, Configuration.USER_LEVEL);
                    break;
                case 1:
                    makeUser(sc,console,Configuration.ROOT_USER_LEVEL);
                    break;
                case 2:
                    changePassword(sc,console);

                    break;
                default:
                    break;


            }

        }

    }


    private static void makeUser(Scanner sc,Console console, byte level) throws IOException, NoSuchAlgorithmException {

        String username;
        char[] password;
        System.out.print("Please enter username: ");
        username = sc.next();

        if (console != null) {
        do {
            password = console.readPassword("Please enter password: ");
        }
        while (!Arrays.equals(password, console.readPassword("Please re-enter password: ")));
    } else {
        do {
            System.out.print("Please enter password: ");
            password = sc.next().toCharArray();
            System.out.print("Please re-enter password: ");
        }
        while (!Arrays.equals(password, sc.next().toCharArray()));
    }

        if(UserInfoManager.registerAPIUser(username,password,level))
        {
            System.out.println("User: "+username+" successfully added");
        }
        else
        {
            System.out.println("The user alread exists");
        }
    }


    private static void changePassword(Scanner sc,Console console) throws IOException, NoSuchAlgorithmException {

        String username;
        char[] password;
        System.out.print("Please enter username: ");
        username = sc.next();

        if (console != null) {
            do {
                password = console.readPassword("Please enter password: ");
            }
            while (!Arrays.equals(password, console.readPassword("Please re-enter password: ")));
        } else {
            do {
                System.out.print("Please enter password: ");
                password = sc.next().toCharArray();
                System.out.print("Please re-enter password: ");
            }
            while (!Arrays.equals(password, sc.next().toCharArray()));
        }


        if(UserInfoManager.changeAPIUserPassword(username,password))
        {
            System.out.println("Password of "+username+" successfully changed");
        }
        else
        {
            System.out.println("The user doesn't exist");
        }
    }
}
