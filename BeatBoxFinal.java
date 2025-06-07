import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static javax.sound.midi.ShortMessage.*;
public class BeatBoxFinal {
    private JList<String> incomingList;
    private JTextArea userMessage;
    private ArrayList<JCheckBox> checkBoxList;
    private Vector<String> listVector = new Vector<>();
    private HashMap<String, boolean[]> otherSqsMap = new HashMap<>();
    private String userName;
    private int nextNum;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Sequencer sequencer;
    private Sequence sequence;
    private Track track;
    String[] instrumrntNames = {"Bass Drum", "Closed Hi-Hat",
            "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap",
            "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga",
            "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo",
            "Open Hi Conga"};
    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args) {
        new BeatBoxFinal().setUp(args[0]);
    }

    public void setUp(String name) {
        userName = name;
        try {
            Socket socket = new Socket("127.0.0.1", 4242);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(new RemoteReader());
        } catch (Exception ex) {
            System.out.println("Couldn’t connect-you’ll have to play alone.");
        }
        setUpMidi();
        buildGUI();
    }

    public void buildGUI() {
        JFrame frame = new JFrame("Cyber Beat Box");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        Box boxButton = new Box(BoxLayout.Y_AXIS);
        JButton start = new JButton("Start");
        start.addActionListener(e -> buildTrackAndStart());
        JButton stop = new JButton("Stop");
        stop.addActionListener(e -> sequencer.stop());
        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(e -> changeTempo(1.03f));
        JButton downTempo = new JButton("Down Tempo");
        downTempo.addActionListener(e->changeTempo(0.97f));
        boxButton.add(start);
        boxButton.add(stop);
        boxButton.add(upTempo);
        boxButton.add(downTempo);
        JButton sendIt = new JButton("Send It");
        sendIt.addActionListener(e -> sendMessageAndTracks());
        boxButton.add(sendIt);
        userMessage = new JTextArea();
        userMessage.setLineWrap(true);
        userMessage.setWrapStyleWord(true);
        JScrollPane scroller = new JScrollPane(userMessage);
        boxButton.add(userMessage);
        incomingList = new JList<>();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        boxButton.add(theList);
        incomingList.setListData(listVector);
        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (String instrumentName : instrumrntNames) {
            JLabel instrumentLabel = new JLabel(instrumentName);
            instrumentLabel.setBorder(BorderFactory.createEmptyBorder(4, 1, 4, 1));
            nameBox.add(instrumentLabel);
        }
        background.add(BorderLayout.EAST, boxButton);
        background.add(BorderLayout.WEST, nameBox);
        frame.getContentPane().add(background);
        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);
        JPanel mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);
        checkBoxList = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkBoxList.add(c);
            mainPanel.add(c);
        }
        frame.setBounds(50, 50, 300, 300);
        frame.pack();
        frame.setVisible(true);
    }

    private void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void buildTrackAndStart() {
        ArrayList<Integer> trackList;
        sequence.deleteTrack(track);
        track = sequence.createTrack();
        for (int i = 0; i < 16; i++) {
            trackList = new ArrayList<>();
            int key = instruments[i];
            for (int j = 0; j < 16; j++) {
                JCheckBox jc = checkBoxList.get(j + 16 * i);
                if (jc.isSelected()) {
                    trackList.add(key);
                } else {
                    trackList.add(null);
                }
            }
            makeTrack(trackList);
            track.add(makeEvent(CONTROL_CHANGE, 1, 127, 0, 16));
        }
        track.add(makeEvent(PROGRAM_CHANGE, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.setTempoInBPM(120);
            sequencer.start();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void changeTempo(float tempoMultiplier) {
        float tempoFactor = sequencer.getTempoFactor();
        sequencer.setTempoFactor(tempoFactor * tempoMultiplier);
    }

    private void sendMessageAndTracks() {
        boolean[] checkBoxState = new boolean[256];
        for (int i = 0; i < 256; i++) {
            JCheckBox check = checkBoxList.get(i);
            if (check.isSelected()) {
                checkBoxState[i] = true;
            }
        }
        try {
            out.writeObject(userName + nextNum++ + " : " + userMessage.getText());
            out.writeObject(checkBoxState);
        } catch (IOException ex) {
            System.out.println("Terribly sorry. Could not send it to the server.");
            ex.printStackTrace();
        }
        userMessage.setText("");
    }
   class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent lse){
            if(!lse.getValueIsAdjusting()){
                String selected = incomingList.getSelectedValue();
                if(selected!=null){
                    boolean [] selectedState=otherSqsMap.get(selected);
                    changeSequence(selectedState);
                    sequencer.stop();
                    buildTrackAndStart();
                }
            }
        }
    }
    private void changeSequence(boolean [] checkboxState){
        for(int i=0;i<256;i++){
            JCheckBox check = checkBoxList.get(i);
            check.setSelected(checkboxState[i]);
        }
    }
    public void  makeTrack(ArrayList<Integer> list){
        for(int i=0;i<list.size();i++){
            Integer instrumentKey = list.get(i);
            if(instrumentKey!=null){
                track.add(makeEvent(NOTE_ON,9,instrumentKey,100,i));
                track.add(makeEvent(NOTE_OFF,9,instrumentKey,100,i+1));
            }
        }
    }
    public static MidiEvent makeEvent(int cmd,int chnl,int one,int two,int tick){
        MidiEvent event=null;
        try{
            ShortMessage msg = new ShortMessage();
            msg.setMessage(cmd,chnl,one,two);
            event = new MidiEvent(msg,tick);
        }
        catch (Exception exception){
            exception.printStackTrace();
        }
        return event;
    }
    class RemoteReader implements Runnable{
        public void run(){
            try{
            Object obj;
            while((obj=in.readObject()) != null){
                System.out.println("Got an Object from server");
                System.out.println(obj.getClass());
                String nameToShow=(String) obj;
                boolean [] checkBoxState= (boolean[]) in.readObject();
                otherSqsMap.put(nameToShow,checkBoxState);
                listVector.add(nameToShow);
                incomingList.setListData(listVector);
                }
        }
            catch (IOException | ClassNotFoundException exception){
                exception.printStackTrace();
            }
        }
    }
}
