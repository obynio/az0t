package com.sybiload;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.*;

public class Main {

    // global variables
    private static String username;
    private static String password;
    private static String zeroshell;
    private static String delay;
    private static String realm;
    private static String section;

    private static String encodedAuthKey;

    public static void main(String[] args)
    {
        System.out.println("\naz0t v0.6 by sybiload\n");

        if (args.length != 0)
        {
            if (args[0].equals("-c") || args[0].equals("--config"))
            {
                // create default xml configuration
                System.out.print("generating default config.. ");
                System.out.println(createXml() ? "success" : "failed");
                System.exit(0);
            }
            else if (args[0].equals("-m") || args[0].equals("--monitor"))
            {
                // starting loop sequence
                System.out.print("parse config.. ");

                if (parseXmlConfig())
                {
                    System.out.println("success");
                    monitor();
                }
                else
                    System.out.println("failed");
            }
            else if (args[0].equals("-h") || args[0].equals("--help"))
            {
                // show help message
                System.out.println("\nusage: az0t.jar <operation>\n");
                System.out.println("operations:");
                System.out.println("az0t.jar -c      generate default configuration file");
                System.out.println("az0t.jar -h      show this help message");
                System.out.println("az0t.jar -m      start the monitor mode\n");
                System.out.println("az0t was created by sybiload under GNU-GPL v3.0 license");
                System.out.println("az0t uses the jsoup library distributed under the MIT license\n");
            }
            else
            {
                // if the user gave invalid arguments
                System.out.println("invalid option : " + args[0]);
            }
        }
        else
        {
            // if no arguments
            System.out.println("no operation specified (use -h for help)");
        }
    }

    private static void monitor()
    {
        try
        {
            while (true)
            {
                System.out.print("auth attempt.. ");

                if (authenticate() && connect())
                {
                    System.out.println("success");

                    long lastTime;

                    do
                    {
                        lastTime = getUnixEpoch();

                        System.out.print("renw attempt.. ");

                        if (renew())
                        {
                            System.out.println("success");

                            for (int i = 0; i < 10; i++)
                            {
                                // in case of the computer goes out of sleep, avoid the delay before new authentication
                                if (getUnixEpoch() - lastTime < Long.parseLong(delay) * 2)
                                    Thread.sleep(Long.parseLong(delay) / 10);
                                else
                                    break;
                            }

                        }
                        else
                        {
                            System.out.println("failed");
                            encodedAuthKey = null;
                            break;
                        }
                    } while (getUnixEpoch() - lastTime < Long.parseLong(delay) * 2);
                }
                else
                {
                    System.out.println("failed");
                }

                Thread.sleep(Long.parseLong(delay));
            }
        }
        catch (Exception e)
        {
            System.out.println("fatal error, aborting..");
        }
    }

    private static String sendPost(String url, String params) throws Exception
    {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");


        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(params);
        wr.flush();
        wr.close();

        // getting html response
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null)
        {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    // encode the authKey for Zeroshell url protocol
    static String encodeAuthKey(String authKey) throws Exception
    {
        // indeed, the authKey must be encoded to url format specs and MUST be separated by \r\n (CRLF, represented by %0A%0D in hexa) every 64 packet length
        String first = authKey.substring(0, 64);
        String second = authKey.substring(64, authKey.length());

        String end = first + "\r\n" + second;

        return URLEncoder.encode(end, "UTF-8");
    }

    static String parseAuthKey(String html)
    {
        // parse auth_key value from the html string
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        org.jsoup.nodes.Element el = doc.select("input[name=Authenticator]").first();

        return el.attr("value");
    }

    private static long getUnixEpoch()
    {
        return System.currentTimeMillis();
    }

    private static boolean authenticate()
    {
        try
        {
            String responseAuth = sendPost(zeroshell, "U=" + username + "&P=" + password + "&Realm=" + realm + "&Action=Authenticate&Section=CPAuth&ZSCPRedirect=_:::_");

            if (responseAuth.contains("successfully authenticated"))
            {
                encodedAuthKey = encodeAuthKey(parseAuthKey(responseAuth));
                return true;
            }
            else
                return false;
        }
        catch (Exception e)
        {
            return false;
        }

    }

    private static boolean connect()
    {
        try
        {
            String responseConnect = sendPost(zeroshell, "U="+ username + "&Section=" + section + "&Realm=" + realm + "&Action=Connect&ZSCPRedirect=_:::_&Authenticator=" + encodedAuthKey);

            return responseConnect.contains("simultaneous") || !responseConnect.contains("Access Denied");
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static boolean renew()
    {
        try
        {
            sendPost(zeroshell, "U=" + username + "&Section=" + section + "&Realm=" + realm + "&Action=Renew&Authenticator=" + encodedAuthKey);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    // parse the configuration
    private static boolean parseXmlConfig()
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File("config.xml"));

            doc.getDocumentElement().normalize();

            username = doc.getElementsByTagName("username").item(0).getTextContent();
            password = doc.getElementsByTagName("password").item(0).getTextContent();
            zeroshell = doc.getElementsByTagName("zeroshell").item(0).getTextContent();
            delay = doc.getElementsByTagName("delay").item(0).getTextContent() + "000";
            realm = doc.getElementsByTagName("realm").item(0).getTextContent();
            section = doc.getElementsByTagName("section").item(0).getTextContent();

            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    // create a default xml configuration
    private static boolean createXml()
    {
        try
        {
            // doing normal stuff
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            // root element
            Element root = document.createElement("root");
            document.appendChild(root);

            // username element
            Node username = document.createElement("username");
            username.appendChild(document.createTextNode("example"));
            root.appendChild(username);

            // password element
            Node password = document.createElement("password");
            password.appendChild(document.createTextNode("example"));
            root.appendChild(password);

            // login page adress without the parameters
            Node zeroshell = document.createElement("zeroshell");
            zeroshell.appendChild(document.createTextNode("http://192.168.0.1:12082/cgi-bin/zscp"));
            root.appendChild(zeroshell);

            // delay in ms element
            Node delay = document.createElement("delay");
            delay.appendChild(document.createTextNode("40"));
            root.appendChild(delay);

            // realm parameter
            Node realm = document.createElement("realm");
            realm.appendChild(document.createTextNode("arpej.com"));
            root.appendChild(realm);

            // section parameter
            Node section = document.createElement("section");
            section.appendChild(document.createTextNode("CPGW"));
            root.appendChild(section);

            // create the xml default file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(document);

            StreamResult streamResult = new StreamResult(new File("config.xml"));
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(domSource, streamResult);

            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }
}

