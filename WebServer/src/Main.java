import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws IOException {
        try {
            //FileInputStream fis;
            //Path path;

            Properties prop = new Properties();
            String configFile = "config.conf";

            try {
                FileInputStream in = new FileInputStream(configFile);
                prop.load(in);
            } catch (FileNotFoundException e) {
                System.out.println("config file not found");
            }

            String ip;
            int port;

            ip = prop.getProperty("ip");
            System.out.println("IP : " + ip);
            port = Integer.parseInt(prop.getProperty("port"));

            InetAddress addr = InetAddress.getByName(ip);

            // create server socket
            ServerSocket server = new ServerSocket(port, 0, addr);
            while(true) {

                System.out.println("Server started in port " + port + ".");

                // listen for client
                Socket client = server.accept();

                String hostName = client.getInetAddress().getHostName();
                System.out.println("Host Name : " + hostName);

                String Direktori = " ";
                if (hostName.equals(prop.getProperty("hostname1"))) {
                    Direktori = new String(prop.getProperty("direktori1"));
                    System.out.println("DocumentRoot : " + Direktori);
                } else if (hostName.equals(prop.getProperty("hostname2"))) {
                    Direktori = new String(prop.getProperty("direktori2"));
                    System.out.println("Direktori : " + Direktori);
                }
                else {
                    Direktori = new String(prop.getProperty("direktori1"));
                    System.out.println("DocumentRoot : " + Direktori);
                }

                System.out.println("Client connected.");

                // create input and output streams
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

                // read request
                String message = reader.readLine();
                String urn = message.split(" ")[1];
                System.out.println(urn);
                urn = urn.substring(1);

                // get file name ex. file.pdf or file.html
                String fileName = urn.substring(urn.lastIndexOf("/") + 1);

                // get content type ex. text/html
                Path path = new File(fileName).toPath();
                String mimeType = Files.probeContentType(path);
                String contentType = mimeType;
                String fileContent;
                String statusCode;

                // get path of file
                String directoryPath = Direktori + urn;

                // get path of current directory
                String currDirectoryPath = directoryPath.substring(directoryPath.lastIndexOf("/") + 1);

                // request is a directory or file
                if (urn.contains(".")) {

                    // check if file is html or not
                    if(contentType.equals("text/html")) {
                        try {
                            FileInputStream fis = new FileInputStream(directoryPath);
                            fileContent = new String(fis.readAllBytes());

                            statusCode = "200 OK";

                        } catch (FileNotFoundException e) {
                            fileContent = "File not found";
                            statusCode = "404 Not Found";
                        }

                        output.write("HTTP/1.1 " + statusCode + "\r\n");
                        output.write("Content-Type: " + contentType + "\r\n");
                        output.write("Content-Length: " + fileContent.length() + "\r\n\r\n");
                        output.write(fileContent);
                        output.flush();

                    } else {
                        try {
                            FileInputStream fis = new FileInputStream(directoryPath);
                            fileContent = new String(fis.readAllBytes());

                            statusCode = "200 OK";

                            System.out.println("File found");
                            output.write("HTTP/1.1 " + statusCode + "\r\n");
                            output.write("Content-Type: " + contentType + "\r\n");
                            output.write("Content-Length: " + fileContent.length() + "\r\n");
                            output.write("Content-Disposition: attachment; filename=" + "\"" + fileName + "\"" + "\r\n\r\n");
                            output.write(fileContent);
                            output.flush();

                        } catch (FileNotFoundException e) {
                            fileContent = "File not found";
                            statusCode = "404 Not Found";

                            System.out.println("File not found");
                            output.write("HTTP/1.1 " + statusCode + "\r\n\r\n");
                            output.write(fileContent);
                            output.flush();
                        }
                    }
                } else {
                    File dirPath = new File(directoryPath);

                    //List of all files and directories
                    File[] files = dirPath.listFiles();
                    if (files != null) {
                        String fileContent2 = "";
                        fileContent2 += "<html>\r\n"
                                + "<body>"
                                + "<table>"
                                + "<tr>"
                                + "<th>Name</th>"
                                + "<th>Last Modified</th>"
                                + "<th>Size</th>"
                                + "</tr>";

                        for (File file : files) {

                            contentType = Files.probeContentType(new File(file.getName()).toPath());
                            fileContent = file.getName();
                            Date lastModified = new Date(file.lastModified());

                            if (file.getName().equals("index.html")) {
                                FileInputStream fis = new FileInputStream(file);
                                fileContent = new String(fis.readAllBytes());

                                statusCode = "200 OK";

                                output.write("HTTP/1.1 " + statusCode + "\r\n");
                                output.write("Content-Type: " + contentType + "\r\n");
                                output.write("Content-Length: " + fileContent.length() + "\r\n\r\n");
                                output.write(fileContent);
                                output.flush();

                            } else {
                                fileContent2 += "<tr>";
                                fileContent2 += "<td><a href=\"" + currDirectoryPath + "/" + file.getName() + "\">" + file.getName() + "</a></td>";
                                fileContent2 += "<td>" + lastModified + "</td>";
                                fileContent2 += "<td>" + file.length() + " Bytes</td>";
                                fileContent2 += "</tr>";
                            }
                        }

                        fileContent2 += "</table>"
                                + "</body>\r\n"
                                + "</html>";
                        output.write("HTTP/1.1 200 OK" + "\r\n\r\n");
                        output.write(fileContent2);

                    } else {

                        fileContent = "Directory not found";
                        statusCode = "404 Not Found";

                        System.out.println("Directory not found");
                        output.write("HTTP/1.1 " + statusCode + "\r\n\r\n");
                        output.write(fileContent);

                    }
                    output.flush();
                }

                while(message.isEmpty()) {
                    message = reader.readLine();
                }

                client.close();
            }
            // server.close();
        } catch (Exception e) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
