import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.*;
import javax.swing.border.BevelBorder;

public class C extends JFrame {
    //private static String pack = null;
    private static PrintWriter writer;
    public BufferedReader reader;
    // public String reader1;
    public BufferedReader reader2;
    public BufferedReader reader3;
    static String IDc = "123411";
    static String IDtgs = "123422";
    static String IDv = "123433";
    static String ADc = "CUG";
    static byte[] K1 = {1,2,3,4,5,6,7,1};//C的密钥
    static byte[] K2 = {1,2,3,4,5,6,7,2};//C和TGS共享的密钥，应该由AS生产
    static byte[] K3 = {1,2,3,4,5,6,7,3};//C和服务器V共享的密钥，应该由TGS产生
    static byte[] Ktgs = {1,2,3,4,5,6,7,4};//TGS的密钥
    static byte[] Kv = {1,2,3,4,5,6,7,5};//服务器的密钥
    static Socket socket;
    String Ticket1;
    String Ticket2;
    static String str;
    private static JTextArea ta = new JTextArea();// 文本域
    private JTextField tf = new JTextField();// 文本框
    Container cc;

    public C(String title) {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cc = this.getContentPane();
        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(new BevelBorder(BevelBorder.RAISED));
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(ta);
        cc.add(tf, "South");
        tf.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // String str = "test"; //发的消息
                writer.println(tf.getText());
                // writer.println(str);
                ta.append("本机:"+tf.getText()+'\n');
                // ta.append("本机:"+str+'\n');
                ta.setSelectionEnd(ta.getText().length());
                tf.setText("");
            }
        });
    }

    public String connect(String ip, int port, String p) throws Exception {
        String pack = null;
        ta.append("尝试连接\n");
        try {
            socket = new Socket(ip, port);
            ta.append("完成连接\n");
            // reader = new BufferedReader(new InputStreamReader(socket
            // .getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            // reader=new BufferedReader(new
            // InputStreamReader(socket.getInputStream()));
            writer.println(p);
            ta.append("发送数据包     :"+p);
            ta.append("\np发送成功\n");
            pack = getServerMessage(socket);
            //ta.append("这是package2:" + pack + '\n');
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pack;
    }

    public static String package1() throws Exception {
        long TS1 = getTimeStamp();
        String p1 = "00010000" + IDc + IDtgs + TS1;
        ta.append("p1明文:   " + p1 + '\n');
        //ta.append("p1成功发送给AS \n");
        // Client->AS
        return p1;
    }

    public static String package3(String p) throws Exception {
        ta.append("收到AS返回的报文p2\n");
        String p2 = p;
        ta.append("p2密文:" + p2 + '\n');
        String str = DES.decrypt(p2, K1);
        ta.append("p2明文:" + str + '\n');
        // reader1=new BufferedReader(new
        // InputStreamReader(socket.getInputStream()));
        String str1 = str.substring(0, 8); // 首部 2
        String Kctgs = str.substring(8, 16); // AS生成的C和tgs之间的会话密钥
        String IDtgs = str.substring(16, 22); // IDtgs
        String TS2 = str.substring(22, 35);// TS2
        String Ticket1 = str.substring(35, 91);// Ticket1
        String Lifetime2 = str.substring(91,93); // 票据生存周期

        long TS3 = getTimeStamp();
        String A1 = IDc + ADc + TS3;
        ta.append("Authenticator_tgs明文:   " + A1 + '\n');
        byte[] Kc_tgs=Kctgs.getBytes();//将生成的8位密钥转成符合DES加密的byte数组
        String Authenticator_tgs = DES.encrypt(A1, Kc_tgs);
        ta.append("Authenticator_tgs密文:   " + Authenticator_tgs + '\n');
        String p3 = "00110000" + IDv + Ticket1 + Authenticator_tgs;
        ta.append("p3明文:" + p3 + '\n');
        //ta.append("p3成功发送给TGS \n");
        return p3;

    }

    public static String package5(String p) throws Exception {
        ta.append("收到TGS返回的报文p4\n");
        String p4 = p;
        ta.append("p4密文:   " + p4 + '\n');
        String Kctgs = IDc.substring(0,4)+IDtgs.substring(2,6);
        byte[] Kc_tgs=Kctgs.getBytes();
        String str = DES.decrypt(p4, Kc_tgs);//解密P4
        ta.append("p4明文:   " + str + '\n');
        String str1 = str.substring(0, 8); // 首部 4
        String str2 = str.substring(8, 16); // Kcv
        String str3 = str.substring(16,22); // IDv
        String TS4 = str.substring(22,35);// TS4
        String Ticket2 = str.substring(35, 91);// Ticket2

        long TS5 = getTimeStamp();
        String Kcv=IDc.substring(0, 4)+IDv.substring(2, 6);//C和V之间的8位密钥，IDc的前四位+IDv的后四位；
        byte[] K_cv=Kcv.getBytes();//转换成符合DES加密的byte密钥
        String A2 = IDc + ADc + TS5;
        String Authenticator_v = DES.encrypt(A2, K_cv);//要给V的认证信息，采用C和V共享的密钥K3，K3应该由TGS产生
        String p5 = "01010000" + Ticket2 + Authenticator_v;
        // Client->V
        writer.println(p5);
        ta.append("p5明文:   " + p5 + '\n');
        //ta.append("p5成功发送给V \n");
        return p5;

    }

    public String getServerMessage(Socket socket) {
        String temp = null;
        String pack = null;

        try {
            reader = new BufferedReader(new InputStreamReader(socket
                    .getInputStream()));
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        try {
            while((temp = reader.readLine())!=null)
            {
                pack = temp;
                pack = pack + reader.readLine();
            }

        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        ta.append("本机收到消息:" + pack + '\n');

        try {
            if (reader != null) {
                reader.close();
            }
            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        //ta.append("返回的reader1:" + pack + '\n');
        return pack;
    }

    private void connectserver(){
        ta.append("尝试连接\n");
        try{
            socket=new Socket("192.168.43.162",4548);
            writer=new PrintWriter(socket.getOutputStream(),true);

            ta.append("完成连接\n");
            reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            getserverMessage();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void getserverMessage(){
        try{
            while(true){
                ta.append("服务器:"+reader.readLine()+"\n");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        try{
            if(reader!=null){
                reader.close();
            }
            if(socket!=null){
                socket.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }


    public boolean Step3() throws Exception // 应用服务器

    {
        long TS5 = getTimeStamp();
        String A2 = IDc + ADc + TS5;
        String Authenticator_v = DES.encrypt(A2, K2);
        String p5 = "0101000" + Ticket2 + Authenticator_v;
        // Client->V
        writer.println(p5); // 发送p5

        while (true) {
            String reader3 = reader.readLine(); // 输入
            String str = DES.decrypt(reader3, K3);
            String str1 = str.substring(0, 8); // 首部 6
            String str2 = str.substring(8, 21); // TS6

            if (str1.equals("01100000")) {
                String str11 = "收到包p6"; // 发的消息
                writer.println(str11);
                ta.append("本机:" + str11 + '\n');
            }
        }
    }

    public static long getTimeStamp() // 时间戳
    {
        long now = System.currentTimeMillis();
        return now;
    }

    public static void main(String[] args) throws Exception {
        C clien = new C("客户机");
        String ip1 = "192.168.43.202";
        String ip2 = "192.168.43.226";
        String ip3 = "192.168.43.162";
        clien.setSize(400, 300);
        clien.setVisible(true);
        String p1 = package1();
        String p2 = clien.connect(ip1, 4546, p1);
        //ta.append("数据包p1  "+p1+"\n");
        //ta.append("数据包p2  "+p2+"\n");
        String p3 = package3(p2);
        //ta.append("数据包p3  "+p3+"\n");
        String p4 = clien.connect(ip2, 4547, p3);
        //ta.append("数据包p4  "+p4+"\n");
        String p5 = package5(p4);
        //ta.append("数据包p5  "+p5+"\n");
        String p6 = clien.connect(ip3, 4548, p5);
        //ta.append("数据包p6  "+p6+"\n");
        clien.connectserver();

    }

}
