import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

public class TGS extends JFrame {
    public PrintWriter writer;
    public BufferedReader reader;
    public ServerSocket server;
    private Socket socket;
    String IDtgs = "123422";
    String IDc = "123411";
    String IDv = "123433";
    String ADc = "CUG";
    String ADv = "Server";
    String str;
    String p2;
    private JTextArea ta = new JTextArea();// 文本域
    private JTextField tf = new JTextField();// 文本框
    Container cc;

    public TGS(String title) {
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

                ta.setSelectionEnd(ta.getText().length());
                tf.setText("");
            }
        });
    }

    public void getserver() {
        try {
            server = new ServerSocket(4547);
            ta.append("TGS服务器套接字已经创建成功\n");
            ta.append("等待客户机的连接\n");
            int i = 0;
            while (true) {
                socket = server.accept();
                i++;
                Client c = new Client(i, socket);
                Thread t = new Thread(c); // 创建客户端处理线程
                t.start(); // 启动线程

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getClientMessage() {
        try {

            String str = reader.readLine(); // 输出
            ta.append("客户机:" + str + '\n');

            byte[] K1 = {1,2,3,4,5,6,7,1};//C的密钥
            byte[] K2 = {1,2,3,4,5,6,7,2};//C和TGS共享的密钥，应该由AS生产
            byte[] K3 = {1,2,3,4,5,6,7,3};//C和服务器V共享的密钥，应该由TGS产生
            byte[] Ktgs = {1,2,3,4,5,6,7,4};//TGS的密钥
            byte[] Kv = {1,2,3,4,5,6,7,5};//服务器的密钥

            String str1=str.substring(0,8);  //首部     3
            //ta.append("首部:" + str1 +"\n");
            String str2=str.substring(8,14); //IDv
            //ta.append("IDv:" + str2 +"\n");
            String str3=str.substring(14,70);//Ticket1
            //ta.append("Ticket1:" + str3 +"\n");
            String str4=str.substring(70,102);//A1，Authenticator_tgs,经过DES加密后成为32位密文
            //ta.append("首部:" + str1 +"\n");

            if(str1.equals("00110000")){
                ta.append("收到C发过来的package p3 \n");
                String Ticket1=DES.decrypt(str3,Ktgs);   //解密
                String str11=Ticket1.substring(0,8);  //Kctgs C和TGS的会话密钥
                ta.append("Kctgs:" + str11 +"\n");
                String str12=Ticket1.substring(8,14); //IDc
                ta.append("IDc:" + str12 +"\n");
                String str13=Ticket1.substring(14,17);//ADc
                ta.append("ADc:" + str13 +"\n");
                String str14=Ticket1.substring(17,23);//IDtgs
                ta.append("IDtgs:" + str14 +"\n");
                String str15=Ticket1.substring(23,36);  //TS2
                ta.append("TS2:" + str15 +"\n");
                String str16=Ticket1.substring(36,38);  //Lifetime2
                ta.append("LT2:" + str16 +"\n");

                byte[] Kc_tgs=str11.getBytes();//将生成的8位密钥转成符合DES加密的byte数组

                String Authenticator_tgs=DES.decrypt(str4,Kc_tgs);   //解密
                String str21=Authenticator_tgs.substring(0,6);  //IDc     A_tgs
                String str22=Authenticator_tgs.substring(6,9); //ADc
                String str23=Authenticator_tgs.substring(9,22);//TS3


                long TS4 = getTimeStamp();
                @SuppressWarnings("unused")
				/*
				 * TGS这边需要生成一个C和V之间的会话密钥，Kc_v
				 * */
                        String Kcv=IDc.substring(0, 4)+IDv.substring(2, 6);//C和V之间的8位密钥，IDc的前四位+IDv的后四位；
                //byte[] K_cv=Kcv.getBytes();//转换成符合DES加密的byte密钥
                String Lifetime4 = "10";
                String str111=Kcv+IDc+ADc+IDv+TS4+Lifetime4;  //去访问V的票据拼接完成
                String Ticket2=DES.encrypt(str111,Kv);		//用V的密钥加密后的票据Ticket2
                String p="01000000"+Kcv+IDv+TS4+Ticket2;  //发送的第四个数据包完成
                ta.append("发给客户机的报文明文为:" + p + "\n");
                String p4 = DES.encrypt(p, Kc_tgs);           //用Kc_tgs加密后形成最终的发送包p4

                writer.println(p4);
                ta.append("发给客户机的报文密文p2为:" + p4 + "\n");// 发送package4
                ta.append("成功发送package p4\n");// 发送package4
                // ta.append("发送package p2\n");
                // ta.setSelectionEnd(ta.getText().length());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
    }

    private final class Client implements Runnable { // 多线程
        int clientIndex = 0; // 保存客户端id
        Socket s = null; // 保存客户端Socket对象

        Client(int i, Socket socket) {
            clientIndex = i;
            this.s = socket;
        }

        public void run() {

            try {
                ta.append("客户端连接成功：\n");
                reader = new BufferedReader(new InputStreamReader(socket
                        .getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                getClientMessage();
            } catch (Exception e) {
                System.out.println("客户端断开连接！");
            }
        }
    }

    public long getTimeStamp() // 时间戳
    {
        long now = System.currentTimeMillis();
        return now;
    }

    public static void main(String[] args) {
        TGS tcp = new TGS("服务器");
        tcp.setSize(400, 300);
        tcp.setVisible(true);
        tcp.getserver();
        //tcp.getClientMessage();

    }

}


