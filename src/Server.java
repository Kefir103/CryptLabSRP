import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.zip.CRC32;

public class Server {

    int v;
    int A;
    int B;
    long S;
    int b;
    int K;
    int M;
    int clientM;
    int R;
    int clientR;

    int N = 1097; // Честно признаюсь взял у одногруппника значения N и G, но лабу делал сам, I swear
    int g = 11;
    int k = 3; // Const

    int U;

    boolean programEnd = false;

    String salt;
    String username;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        Server main = new Server();
        System.out.println("Started: " + serverSocket);
        while (true){
            try{
                Socket client = serverSocket.accept();
                try{
                    System.out.println("Socket: " + client + " connected!");
                    DataInputStream input = new DataInputStream(client.getInputStream());
                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    System.out.println("Registration...");
                    while (input.readUTF() == null){ }
                    System.out.println("Server recieved from client: ");
                    main.username = input.readUTF();
                    System.out.println("Username: " + main.username);
                    main.salt = input.readUTF();
                    System.out.println("Salt: " + main.salt);
                    main.v = input.readInt();
                    System.out.println("V: " + main.v);

                    System.out.println("Authentication...");

                    // Проверка правильности логина
                    while (!input.readUTF().equals(main.username)){
                        System.out.println("Username from client is wrong!");
                        if (input.readUTF().equals(main.username)){
                            System.out.println("User logon with name: " + main.username);
                            break;
                        }
                    }

                    // Получение А от клиента
                    main.A = input.readInt();
                    main.checkA(main.A);

                    // Вычисление В и отправка клиенту
                    main.setB(main.k, main.g, main.N, main.v);
                    out.writeInt(main.B);
                    out.flush();

                    System.out.println("b = " + main.b);
                    out.writeInt(main.b);
                    out.flush();

                    main.U = main.hash("" + main.A + main.B);
                    if (main.U == 0){
                        System.out.println("U == 0... Sorry, but I must close connection");
                        client.close();
                    }
                    System.out.println("U = " + main.U);

                    System.out.println("A = " + main.A +
                            "\nv = " + main.v +
                            "\nU = " + main.U +
                            "\nN = " + main.N +
                            "\nb = " + main.b);
                    main.S = main.setS(main.A, main.v, main.U, main.N, main.b);
                    System.out.println("S = " + main.S);

                    main.K = main.hash(""+main.S);
                    System.out.println("K = " + main.K);

                    main.M = main.setM(main.S, main.A, main.B, main.K);
                    System.out.println("M = " + main.M);
                    main.clientM = input.readInt();

                    if (main.clientM == main.M){
                        System.out.println("M(client) == M(server)");
                    } else {
                        System.out.println("M(client) != M(server), so I close connection");
                        client.close();
                    }

                    main.R = main.hash("" + main.A + main.M + main.K);
                    main.clientR = input.readInt();
                    if (main.clientR == main.R){
                        System.out.println("R(client) == R(server)");
                        main.programEnd = true;
                    } else {
                        System.out.println("R(client) != R(server), so I close connection");
                        client.close();
                    }
                } finally {
                    if (main.programEnd){
                        System.out.println("Everything is OK");
                    }
                    else {
                        System.out.println("Something wrong");
                    }
                    client.close();
                }
            } finally {
                serverSocket.close();
            }
        }

    }

    private void checkA(int a) throws IOException {
        if (a == 0){
            System.out.println("A == 0");
            throw new IOException("A == 0");
        } else {
            System.out.println("A = " + a);
        }
    }

    void setB(int k, int g, int N, int v) throws IOException{
        Random random = new Random();
        this.b = random.nextInt(12); // 1-байтное значение
        int tmpKV = k * v;
        int tmpGBModN = 1;
        for (int i = 0; i < b; i++){
            tmpGBModN *= g;
            tmpGBModN %= N;
        }
        this.B = (tmpKV + tmpGBModN) % N;

        if (this.B == 0){
            System.out.println("B == 0");
            throw new IOException("B == 0");
        } else {
            System.out.println("B = " + this.B);
        }
    }

    int hash (String data){
        byte[] sha = DigestUtils.sha(data);
        long hash;
        CRC32 crc32 = new CRC32();
        crc32.update(sha);
        hash = crc32.getValue();
        hash %= 1000; // Сократим время обработки, а то очень долго в степень возводит
        return (int) hash;
    }

    long setS(int A, int v, int U, int N, int b){
        long tmpVU = 1;
        for (int i = 0; i < U; i++){
            tmpVU *= v;
            tmpVU %= N;
        }
        long tmpAVU = A * tmpVU;
        long S = 1;
        for (int i = 0; i < b; i++){
            S *= tmpAVU;
            S %= N;
        }
        return S;
    }

    int setM(long S, int A, int B, int K){
        int tmpHashN = hash("" + this.M);
        int tmpHashG = hash("" + this.g);
        int tmpHashI = hash(this.username);
        int tmpXOR = tmpHashN ^ tmpHashG;
        int M = hash("" + tmpXOR + tmpHashI + S + A + B + K);
        return M;
    }
}
