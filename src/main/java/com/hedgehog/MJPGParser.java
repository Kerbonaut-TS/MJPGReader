package com.hedgehog;


import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;

public class MJPGParser {

    private String ip_address;
    private String username;
    private String password;

    private BufferedInputStream input;
    private ByteArrayOutputStream frame;
    private DataReader reader;

    private int section;
    private static int IGNORE = 0;
    private static int HEADER = 1;
    private static int IMAGE = 2;
    private static int END = 3;

    //output
    public String header;

    public MJPGParser(String ip_address, String username, String password) throws FileNotFoundException, IOException {
        /* Constructor with username and password */

        this.username = username;
        this.password = password;
        Authenticator.setDefault(new HTTPAuthenticator(username, password));
        this.ip_address = ip_address;
        this.section = IGNORE;

    }

    public MJPGParser(String ip_address) throws FileNotFoundException, IOException {
        /* Constructor without  Authentication */
        this.ip_address = ip_address;
        this.section = IGNORE;
    }

    public void connect() throws IOException {
        URL url = new URL(this.ip_address);
        this.input = new BufferedInputStream(url.openStream());
    }






    public int assess_section(String s) throws IOException {


        if ((this.section == IGNORE) && (s.contains("bound") || s.contains("cross"))) {
            return HEADER;

        } else if (this.section == HEADER ) {

            if(reader.JPG_begins()) {
                //DEBUGGING display header
                System.out.println(this.header);
                this.frame.write((byte) 0xFF);
                this.frame.write((byte) 0xD8);
                return IMAGE;

            }else{
                return HEADER;
            }

        } else if (this.reader.JPG_ends()) {

            return END;

        }

        return this.section;
    }


    public BufferedImage grab_frame() throws IOException, InterruptedException {

        String string_buffer = "";
        this.reader = new DataReader();
        this.frame = new ByteArrayOutputStream();

        while (this.section != END) {

            if (this.section == IGNORE) {
                this.reader.read(this.input, 8);
                string_buffer = string_buffer + reader.getString();
                this.section = this.assess_section(string_buffer);

            }else if (this.section == HEADER) {

                this.reader.read(this.input, 1);
                this.header = this.header + reader.getString();
                this.section = this.assess_section(header);


            } else if (this.section == IMAGE) {

                this.reader.read(this.input, 1);
                this.section = this.assess_section(reader.getString());
                this.frame.write(reader.getBytes(), 0, 1);


            }


        }

        ByteArrayInputStream bais = new ByteArrayInputStream(this.frame.toByteArray());
        BufferedImage image = ImageIO.read(bais);
        this.next_frame();

        return image;

    }

    private void  next_frame(){
        this.section = IGNORE;
        this.header = "";
        this.frame = new ByteArrayOutputStream();
    }


    private  class DataReader {

        byte[] byte_chunk;
        byte previous_byte;
        String parsed_string = "";

        public DataReader() {
        }

        public void read(BufferedInputStream inputstream, int byte_lenght) throws IOException {
            if(byte_chunk != null && byte_chunk.length > 0) previous_byte = byte_chunk[byte_chunk.length - 1];
            byte_chunk = new byte[byte_lenght];
            int read_count = inputstream.read(byte_chunk);
            parsed_string = new String(byte_chunk, 0, read_count);
        }

        public String getString() {
            return parsed_string;
        }

        public byte[] getBytes() {
            return byte_chunk;
        }


        public Boolean JPG_begins (){
            return (byte_chunk[byte_chunk.length - 1] == (byte) 0xD8) && (previous_byte == (byte) 0xFF);
        }

        public Boolean JPG_ends (){
            return (byte_chunk[byte_chunk.length - 1] == (byte) 0xD9) && (previous_byte == (byte) 0xFF);
        }



    }


    class HTTPAuthenticator extends Authenticator {
        private String username, password;

        public HTTPAuthenticator(String user, String pass) {
            username = user;
            password = pass;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            System.out.println("Requesting Host  : " + getRequestingHost());
            System.out.println("Requesting Port  : " + getRequestingPort());
            System.out.println("Requesting Prompt : " + getRequestingPrompt());
            System.out.println("Requesting Protocol: "
                    + getRequestingProtocol());
            System.out.println("Requesting Scheme : " + getRequestingScheme());
            System.out.println("Requesting Site  : " + getRequestingSite());
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }

}//end class
