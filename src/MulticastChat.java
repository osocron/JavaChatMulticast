import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
public class MulticastChat {

    private static final String DEFAULT_GROUP = "239.1.2.3";
    private static final int DEFAULT_PORT = 1234;
    private static final int DEFAULT_TTL = 1;

    private InetAddress group;
    private int port;
    private int ttl;

    private MulticastChat(InetAddress group, int port, int ttl) {
        this.group = group;
        this.port = port;
        this.ttl = ttl;
        initAWT ();
    }
    private Frame frame;
    private TextArea area;
    private TextField field;
    private void initAWT() {
        frame = new Frame
                ("MulticastChat [" + group.getHostAddress () + ":" + port + "]");
        frame.addWindowListener (new WindowAdapter () {
            public void windowOpened (WindowEvent event) {
                field.requestFocus ();
            }
            public void windowClosing (WindowEvent event) {
                try {
                    stop ();
                } catch (IOException ignored) {
                }
            }
        });
        area = new TextArea ("", 12, 24, TextArea.SCROLLBARS_VERTICAL_ONLY);
        area.setEditable (false);
        frame.add (area, "Center");
        field = new TextField ("");
        field.addActionListener (event -> {
            netSend (event.getActionCommand ());
            field.selectAll ();
        });
        frame.add (field, "South");
        frame.pack ();
    }

    private void start() throws IOException {
        netStart ();
        frame.setVisible (true);
    }
    private void stop() throws IOException {
        netStop ();
        frame.setVisible (false);
    }
    private MulticastSocket socket;
    private BufferedReader in;
    private OutputStreamWriter out;
    private Thread listener;
    private void netStart() throws IOException {
        socket = new MulticastSocket (port);
        socket.setTimeToLive (ttl);
        socket.joinGroup (group);
        in = new BufferedReader(new InputStreamReader (new DatagramInputStream (socket), "UTF8"));
        out = new OutputStreamWriter (new DatagramOutputStream (socket, group, port), "UTF8");
        listener = new Thread(this::netReceive);
        listener.start();
    }
    private void netStop() throws IOException {
        listener.interrupt ();
        listener = null;
        socket.leaveGroup (group);
        socket.close ();
    }
    private void netSend(String message) {
        try {
            out.write (message + "\n");
            out.flush ();
        } catch (IOException ex) {
            ex.printStackTrace ();
        }
    }
    private void netReceive() {
        try {
            Thread myself = Thread.currentThread ();
            while (listener == myself) {
                String message = in.readLine ();
                area.append (message + "\n");
            }
        } catch (IOException ex) {
            area.append ("- listener stopped");
            ex.printStackTrace ();
        }
    }

    public static void main (String[] args) throws IOException {
        if ((args.length > 3) || ((args.length > 0) && args[1].endsWith ("help"))) {
            System.out.println
                    ("Syntax: MulticastChat [<group:" + DEFAULT_GROUP +
                            "> [<port:" + DEFAULT_PORT + ">] [<ttl:" + DEFAULT_TTL + ">]]");
            System.exit (0);
        }
        String groupStr = (args.length > 0) ? args[0] : DEFAULT_GROUP;
        InetAddress group = InetAddress.getByName (groupStr);
        int port = (args.length > 1) ? Integer.parseInt (args[1]) : DEFAULT_PORT;
        int ttl = (args.length > 2) ? Integer.parseInt (args[2]) : DEFAULT_TTL;
        MulticastChat chat = new MulticastChat (group, port, ttl);
        chat.start ();
    }
}
