package server.mailbox;

import common.mail.MailWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import server.Log;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


    /*--------------------------------------- ALL MAILBOXES ------------------------------------- */
    /*------- Class to manage and save all user's mailboxes (xml reading and writing) ----------- */

public class AllMailboxes {
    private HashMap<String, Mailbox> all_mailboxes = new HashMap<>(); // <user, mailbox>
    private static final String PATH = "src/server/mailbox/";
    private Log log;

    public AllMailboxes(Log log){
        this.log = log;
    }


    /* Method to initialize a mailbox */
    public void initializeMailbox(String user){
        if (!all_mailboxes.containsKey(user)) {
            all_mailboxes.put(user, new Mailbox(user));
            all_mailboxes.get(user).initialize();
        }
    }
    /* ---------------------------------------- */


    /* Method to return a initial user mail list */
    public ArrayList<MailWrapper> getUserMailList(String user){
        if (!all_mailboxes.containsKey(user))
            initializeMailbox(user);
        all_mailboxes.get(user).new_mail.clear();
        return new ArrayList<>(all_mailboxes.get(user).mail_list.values());
    }
    /* ---------------------------------------- */


    /* Method to return if there are new mail */
    public boolean areNewMails(String user){
        if (!all_mailboxes.containsKey(user))
            initializeMailbox(user);
        return !all_mailboxes.get(user).new_mail.isEmpty();
    }
    /* ---------------------------------------- */


    /* Method to return new mail of user */
    public ArrayList<MailWrapper> getNewMails(String user){
        if (!all_mailboxes.containsKey(user))
            initializeMailbox(user);
        ArrayList<MailWrapper> tmp =  new ArrayList<>(all_mailboxes.get(user).new_mail);
        all_mailboxes.get(user).new_mail.clear();
        return tmp;
    }
    /* ---------------------------------------- */


    /* Method to send a new mail */
    public String sendMail(String user, MailWrapper mail_to_send){
        if (!all_mailboxes.containsKey(user))
            initializeMailbox(user);
        return all_mailboxes.get(user).send(mail_to_send); // return msg
    }
    /* ---------------------------------------- */


    /* Method to delete a mail */
    public void deleteMail(String user, int mail_id){
        if (!all_mailboxes.containsKey(user))
            initializeMailbox(user);
        all_mailboxes.get(user).delete(mail_id);
    }
    /* ---------------------------------------- */



    /* ----------------------------------------------------------------------------------------- */
    /* ----------------------------------------------------------------------------------------- */



    /* --------------------------------- MAILBOX ---------------------------------------------- */
    /* ----------- Internal class representing and managing a single mailbox ------------------ */

    private class Mailbox {
        private HashMap<String, MailWrapper> mail_list = new HashMap<>(); // <id, mail>
        private ArrayList<MailWrapper> new_mail = new ArrayList<>();
        private final String user; // owner
        private ReadWriteLock rwl = new ReentrantReadWriteLock();
        private Lock rl = rwl.readLock();
        private Lock wl = rwl.writeLock();

        private Mailbox(String user){
            this.user = user;
        }


        /* Method that reads the xml file and returns its document */
        private Document readDocument(String user) {
            File xml_file = new File(PATH + user + "/mailbox.xml");
            Document document = null;

            try {
                DocumentBuilderFactory doc_factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder doc = doc_factory.newDocumentBuilder();
                document = doc.parse(xml_file);
            }
            catch (ParserConfigurationException | IOException | SAXException e) {
                log.addLog(e.getMessage());
            }

            return document;
        }
        /* ------------------------------------------------------------------------------ */


        /* Method to updates (writes) the xml file */
        private void writeDocument(String user, Document document){
            try {
                File xml_file = new File(PATH + user + "/mailbox.xml");
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                DOMSource source = new DOMSource(document);
                StreamResult result = new StreamResult(xml_file);
                transformer.transform(source, result);
            } catch (TransformerException e) { log.addLog(e.getMessage()); }
        }
        /* ------------------------------------------------------------------------------ */


        /* Method to create xml file if it doesn't exist */
        private void newDocument(String user) {
            if (!Files.exists(Path.of(PATH + user + "/mailbox.xml"))) {
                File xml_file = new File(PATH + user + "/mailbox.xml");
                try {
                    xml_file.createNewFile();

                    Element root;
                    DocumentBuilderFactory doc_factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder doc = doc_factory.newDocumentBuilder();

                    Document document = doc.newDocument();
                    root = document.createElement("mailbox");
                    document.appendChild(root);
                    writeDocument(user, document);
                } catch (IOException | ParserConfigurationException e) {
                    log.addLog(e.getMessage());
                }
            }
        }
        /* ------------------------------------------------------------------------------ */



        /* -------------------- Method to initialize a mailbox --------------------------- */
        private void initialize() {
            // read the xml file and add common.mail to the list

            wl.lock();
            newDocument(user); // it is created if it does not exist
            wl.unlock();

            rl.lock();
            Document document = readDocument(user);
            rl.unlock();

            if (document != null) {

                NodeList node_list_mail = document.getElementsByTagName("mail");

                // until the mailbox is fully initialized nobody can read
                this.wl.lock();

                for (int i = 0; i < node_list_mail.getLength(); i++) {
                    Element node_element = (Element) node_list_mail.item(i);
                    String id = node_element.getAttribute("id");

                    MailWrapper mail_to_add = new MailWrapper(
                            Integer.parseInt(id),
                            node_element.getElementsByTagName("sender").item(0).getTextContent(),
                            node_element.getElementsByTagName("recipients").item(0).getTextContent(),
                            node_element.getElementsByTagName("object").item(0).getTextContent(),
                            node_element.getElementsByTagName("text").item(0).getTextContent(),
                            node_element.getElementsByTagName("date").item(0).getTextContent());

                    mail_list.put(id,mail_to_add);
                }

                this.wl.unlock();
            }

        }

        /* ---------------------------------------------------------------------------------------- */


        /* -------------------------- Method to send a new mail  ---------------------------------- */
        private String send(MailWrapper mail_to_send) {
            // remove whitespaces from recipients
            String recipients_string = mail_to_send.getRecipients().replaceAll("\\s+","");
            String[] recipients = recipients_string.split(","); // individual recipients
            String msg; // message sent to the client indicating whether the mail has been sent or not


            for (String recipient : recipients) { // for each recipient
                if (!Files.exists(Path.of(PATH + recipient))) { // if there is no recipient, the email is not sent
                    log.addLog(user+" SEND FAILED");
                    msg = "Enter existing email addresses";
                    return msg;
                }
            }

            for (String recipient : recipients) { // for each recipient

                initializeMailbox(recipient);

                all_mailboxes.get(recipient).rl.lock();
                Document document = readDocument(recipient);
                all_mailboxes.get(recipient).rl.unlock();

                Element root = document.getDocumentElement();
                int id;

                all_mailboxes.get(recipient).wl.lock();
                if (root.hasChildNodes()) {
                    // id of the last email
                    id = Integer.parseInt(root.getFirstChild().getAttributes().getNamedItem("id").getNodeValue());
                    id = id + 1;
                } else
                    id = 0;
                all_mailboxes.get(recipient).wl.unlock();

                // Creation of xml email nodes
                Element mail = document.createElement("mail");
                mail.setAttribute("id", Integer.toString(id));
                root.insertBefore(mail, root.getFirstChild());

                Element sender = document.createElement("sender");
                sender.appendChild(document.createTextNode(mail_to_send.getSender()));
                mail.appendChild(sender);

                Element recipients_ = document.createElement("recipients");
                recipients_.appendChild(document.createTextNode(mail_to_send.getRecipients()));
                mail.appendChild(recipients_);

                Element object = document.createElement("object");
                object.appendChild(document.createTextNode(mail_to_send.getObject()));
                mail.appendChild(object);

                Element text = document.createElement("text");
                text.appendChild(document.createTextNode(mail_to_send.getText()));
                mail.appendChild(text);

                Element date = document.createElement("date");
                date.appendChild(document.createTextNode(mail_to_send.getDate()));
                mail.appendChild(date);

                // write xml
                all_mailboxes.get(recipient).wl.lock();
                writeDocument(recipient, document);
                // update list
                mail_to_send.setId(id);
                all_mailboxes.get(recipient).mail_list.put(Integer.toString(id), mail_to_send);
                all_mailboxes.get(recipient).new_mail.add(mail_to_send);
                all_mailboxes.get(recipient).wl.unlock();

                log.addLog(user + " SENT MAIL TO " + recipient);
                log.addLog(recipient + " RECEIVED MAIL FROM " + user);

            }
            msg = "Email/s sent";
            return msg;
        }

        /* ---------------------------------------------------------------------------------------- */


        /* -------------------------- Method to delete a mail ------------------------------------- */
        private void delete(int mail_id) {
            rl.lock();
            Document document = readDocument(user);
            rl.unlock();

            if (document != null){
                Element mailbox = document.getDocumentElement(); // root

                NodeList node_list_mail = document.getElementsByTagName("mail");

                this.wl.lock();

                mail_list.remove(Integer.toString(mail_id)); // remove from the list

                // remove from the xml
                for (int i = 0; i < node_list_mail.getLength(); i++) {
                    int id = Integer.parseInt(node_list_mail.item(i).getAttributes().getNamedItem("id").getNodeValue());
                    if (id == mail_id) {
                        mailbox.removeChild(node_list_mail.item(i));
                        break;
                    }
                }

                this.writeDocument(user, document); // update xml

                this.wl.unlock();

                log.addLog(user+" delete email with id: "+mail_id);

            }
        }
        /* ---------------------------------------------------------------------------------------- */
    }
}
