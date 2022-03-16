package izzulhaq.webserver;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class HttpWorker extends Thread {
    Socket socket;
    String clientRequest;

    public HttpWorker(String clientRequest, Socket socket) {
        this.socket = socket;
        this.clientRequest = clientRequest;
    }

    public void run() {
        try {
            LogUtil.clear();
            PrintStream printer = new PrintStream(socket.getOutputStream());

            LogUtil.write("");
            LogUtil.write("HttpWorker sedang bekerja...");
            LogUtil.write(clientRequest);

            if (!clientRequest.startsWith("GET") || clientRequest.length() < 14 || !(clientRequest.endsWith("HTTP/1.0") || clientRequest.endsWith("HTTP/1.1"))) {
                LogUtil.write("400(Bad Request): " + clientRequest);
                String errorPage = buildErrorPage("400", "Bad Request", "Request tidak dapat dipahami.");
                printer.println(errorPage);
            }
            else {
                String request = clientRequest.substring(4, clientRequest.length()-9).trim();

                if (request.indexOf("..") > -1 || request.indexOf("/.ht") > -1 || request.endsWith("~")) {
                    LogUtil.write("403(Forbidden): " + request);
                    String errorPage = buildErrorPage("403", "Forbidden", "Izin diperlukan untuk mengakses URL " + request);
                    printer.println(errorPage);
                }
                else {
                    if (!request.startsWith("/images/") && !request.endsWith("favicon.ico")) {

                    }

                    request = URLDecoder.decode(request, "UTF-8");

                    if (request.endsWith("/")) {
                        request = request.substring(0, request.length() - 1);
                    }

                    if (request.indexOf(".") > -1) {
                        if (request.indexOf(".fake-cgi") > -1) {
                            LogUtil.write("> [CGI] request...");
                            handleCGIRequest(request, printer);
                        }
                        else {
                            if (!request.startsWith("/images/") && !request.startsWith("/favicon.ico")) {
                                LogUtil.write("> [SINGLE FILE] request...");
                            }
                            handleFileRequest(request, printer);
                        }
                    }
                    else {
                        LogUtil.write("> [DIRECTORY EXPLORE] request...");
                        handleExploreRequest(request, printer);
                    }
                }
            }
            LogUtil.save(true);
            socket.close();
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    private void handleCGIRequest(String request, PrintStream printer) throws UnsupportedEncodingException {
        Map<String, String> params = parseUrlParams(request);

        Integer number1 = tryParse(params.get("num1"));
        Integer number2 = tryParse(params.get("num2"));

        if (number1 == null || number2 == null) {
            String errorMsg = "Invalid parameter: " + params.get("num1") + " or " + params.get("num2") + ", both must be integer!";
            LogUtil.write(">> " + errorMsg);
            String errorPage = buildErrorPage("500", "Internal Server Error", errorMsg);
            printer.println(errorPage);
        }
        else {
            LogUtil.write(">> " + number1 + " + " + number2 + " = " + (number1 + number2));
            StringBuilder sbContent = new StringBuilder();
            sbContent.append("Dear " + params.get("person") + ", the sum of ");
            sbContent.append(params.get("num1") + " and " + params.get("num2") + " is ");
            sbContent.append(number1 + number2);
            sbContent.append(".");
            String htmlPage = buildHtmlPage(sbContent.toString(), "Fake-CGI: Add two numbers");
            String htmlHeader = buildHttpHeader("aa.html", htmlPage.length());
            printer.println(htmlHeader);
            printer.println(htmlPage);
        }
    }

    private void handleFileRequest(String request, PrintStream printer) throws FileNotFoundException, IOException {
        String rootDir = getRootFolder();
        String path = Paths.get(rootDir, request).toString();

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            printer.println("No such resource: " + request);
            LogUtil.write(">> No such resource: " + request);
        }
        else {
            if (!request.startsWith("/images/") && !request.startsWith("/favicon.ico")) {
                LogUtil.write(">> Seek the content of file: " + file.getName());
            }

            String htmlHeader = buildHttpHeader(path, file.length());
            printer.println(htmlHeader);

            InputStream fs = new FileInputStream(file);
            byte[] buffer = new byte[1000];
            while (fs.available() > 0) {
                printer.write(buffer, 0, fs.read(buffer));
            }
            fs.close();
        }
    }

    private void handleExploreRequest(String request, PrintStream printer) {
        String rootDir = getRootFolder();
        String path = Paths.get(rootDir, request).toString();

        File file = new File(path);
        if (!file.exists()) {
            printer.println(">> No such resource: " + request);
            LogUtil.write("No such resource: " + request);
        }
        else {
            LogUtil.write(">> Explore the content under folder: " + file.getName());

            File[] files = file.listFiles();
            Arrays.sort(files);

            StringBuilder sbDirHtml = new StringBuilder();

            sbDirHtml.append("<table>");
            sbDirHtml.append("<tr>");
            sbDirHtml.append("    <th>Name</th>");
            sbDirHtml.append("    <th>Last Modified</th>");
            sbDirHtml.append("    <th>Size(Bytes)</th>");
            sbDirHtml.append("</tr>");

            if (!path.equals(rootDir)) {
                String parent = path.substring(0, path.lastIndexOf(File.separator));
                if (parent.equals(rootDir)) {
                    parent = "../";
                }
                else {
                    parent = parent.replace(rootDir, "");
                }

                parent = parent.replace("\\", "/");

                sbDirHtml.append("<tr>");
                sbDirHtml.append("    <td><img src=\"" + buildImageLink(request, "images/folder.png") + "\"></img><a href=\"" + parent + "\">../</a></td>");
                sbDirHtml.append("    <td></td>");
                sbDirHtml.append("    <td></td>");
                sbDirHtml.append("</tr>");
            }

            List<File> folders = getFileByType(files, true);
            for (File folder : folders) {
                LogUtil.write(">> Directory: " + folder.getName());
                sbDirHtml.append("<tr>");
                sbDirHtml.append("    <td><img src=\"" + buildImageLink(request, "images/folder.png") + "\"></img><a href=\"" + buildRelativeLink(request, folder.getName()) + "\">" + folder.getName() + "</a></td>");
                sbDirHtml.append("    <td>" + getFormattedDate(folder.lastModified()) + "</td>");
                sbDirHtml.append("    <td></td>");
                sbDirHtml.append("</tr>");
            }

            List<File> fileList = getFileByType(files, false);
            for (File f : fileList) {
                LogUtil.write(">> File: " + f.getName());
                sbDirHtml.append("<tr>");
                sbDirHtml.append("    <td><img src=\"" + buildImageLink(request, getFileImage(f.getName())) + "\" width=\"16\"></img><a href=\"" + buildRelativeLink(request, f.getName()) + "\">" + f.getName() + "</a></td>");
                sbDirHtml.append("    <td>" + getFormattedDate(f.lastModified()) + "</td>");
                sbDirHtml.append("    <td>" + f.length() + "</td>");
                sbDirHtml.append("</tr>");
            }

            sbDirHtml.append("</table>");
            String htmlPage = buildHtmlPage(sbDirHtml.toString(), "");
            String htmlHeader = buildHttpHeader(path, htmlPage.length());
            printer.println(htmlHeader);
            printer.println(htmlPage);
        }
    }

    private String buildHttpHeader(String path, long length) {
        StringBuilder sbHtml = new StringBuilder();
        sbHtml.append("HTTP/1.1 200 OK");
        sbHtml.append("\r\n");
        sbHtml.append("Content-length: " + length);
        sbHtml.append("\r\n");
        sbHtml.append("Content-type: " + getContentType(path));
        sbHtml.append("\r\n");
        return sbHtml.toString();
    }

    private String buildHtmlPage(String content, String header) {
        StringBuilder sbHtml = new StringBuilder();
        sbHtml.append("<!DOCTYPE html>");
        sbHtml.append("<html>");
        sbHtml.append("<head>");
        sbHtml.append("<style>");
        sbHtml.append("    table { width:50%; }");
        sbHtml.append("    th, td { padding:3px; text-align:left; }");
        sbHtml.append("</style>");
        sbHtml.append("<title>My Web Server</title>");
        sbHtml.append("</head>");
        sbHtml.append("<body>");
        if (header != null && !header.isEmpty()) {
            sbHtml.append("<h1>" + header + "</h1>");
        }
        else {
            sbHtml.append("<h1>File Explorer in Web Server</h1>");
        }
        sbHtml.append(content);
        sbHtml.append("<hr>");
        sbHtml.append("<p>*This page is returned by Web Server.</p>");
        sbHtml.append("</body>");
        sbHtml.append("</html>");
        return sbHtml.toString();
    }

    private String buildErrorPage(String code, String title, String message) {
        StringBuilder sbHtml = new StringBuilder();
        sbHtml.append("HTTP/1.1 " + code + " " + title + "\r\n\r\n");
        sbHtml.append("<!DOCTYPE html>");
        sbHtml.append("<html>");
        sbHtml.append("<head>");
        sbHtml.append("<title>" + code + " " + title + "</title>");
        sbHtml.append("</head>");
        sbHtml.append("<body>");
        sbHtml.append("<h1>" + code + " " + title + "</h1>");
        sbHtml.append("<p>" + message + "</p>");
        sbHtml.append("<hr>");
        sbHtml.append("<p>*This page is returned by Web Server.</p>");
        sbHtml.append("</body>");
        sbHtml.append("</html>");
        return sbHtml.toString();
    }

    private List<File> getFileByType(File[] fileList, boolean isFolder) {
        List<File> files = new ArrayList<File>();
        if (fileList == null || fileList.length == 0) {
            return files;
        }

        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].isDirectory() && isFolder) {
                files.add(fileList[i]);
            }
            else if (fileList[i].isFile() && !isFolder) {
                files.add(fileList[i]);
            }
        }
        return files;
    }

    private Map<String, String> parseUrlParams(String url) throws UnsupportedEncodingException {
        HashMap<String, String> mapParams = new HashMap<String, String>();
        if (url.indexOf("?") < 0) {
            return mapParams;
        }

        url = url.substring(url.indexOf("?") + 1);
        String[] pairs = url.split("&");
        for (String pair : pairs) {
            int index = pair.indexOf("=");
            mapParams.put(URLDecoder.decode(pair.substring(0, index), "UTF-8"), URLDecoder.decode(pair.substring(index + 1), "UTF-8"));
        }
        return mapParams;
    }

    private String getRootFolder() {
        String root = "";
        try {
            File file = new File(".");
            root = file.getCanonicalPath();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return root;
    }

    private String getFormattedDate(long lastModified) {
        if (lastModified < 0) {
            return "";
        }

        Date lm = new Date(lastModified);
        String lasmod = new SimpleDateFormat("dd-MM-yyy HH:mm:ss").format(lm);
        return lasmod;
    }

    private String buildRelativeLink(String request, String fileName) {
        if (request == null || request.equals("") || request.equals("/")) {
            return fileName;
        }
        else {
            return request + "/" + fileName;
        }
    }

    private String buildImageLink(String request, String fileName) {
        if (request == null || request.equals("") || request.equals("/")) {
            return fileName;
        }
        else {
            String imageLink = fileName;
            for (int i = 0; i < request.length(); i++) {
                if (request.charAt(i) == '/') {
                    imageLink = "../" + imageLink;
                }
            }
            return imageLink;
        }
    }

    private static String getFileImage(String path) {
        if (path == null || path.equals("") || path.lastIndexOf(".") < 0) {
            return "images/file.png";
        }

        String extension = path.substring(path.lastIndexOf("."));
        switch (extension) {
            case ".class":
                return "images/class.png";
            case ".html":
                return "images/html.png";
            case ".java":
                return "images/java.png";
            case ".txt":
                return "images/text.png";
            case ".xml":
                return "images/xml.png";
            default:
                return "images/file.png";
        }
    }

    private static String getContentType(String path) {
        if (path == null || path.equals("") || path.lastIndexOf(".") < 0) {
            return "text/html";
        }

        String extension = path.substring(path.lastIndexOf("."));
        switch (extension) {
            case ".html":
            case ".htm":
                return "text/html";
            case ".txt":
                return "text/plain";
            case ".ico":
                return "image/x-icon.ico";
            case ".wml":
                return "text/html";
            default:
                return "text/plain";
        }
    }

    private Integer tryParse(String text) {
        try {
            return Integer.parseInt(text);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
