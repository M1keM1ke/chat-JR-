package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>(); //мапа для отправки сообщений всем

    private static class Handler extends Thread {
        @Override
        public void run() {
            super.run();
            ConsoleHelper.writeMessage("установлено новое соединение с удаленным адресом: " + socket.getRemoteSocketAddress());
            String userName = "";

            try(Connection newConnection = new Connection(socket)) {

                userName = serverHandshake(newConnection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(newConnection, userName);
                serverMainLoop(newConnection, userName);


            } catch (IOException e) {
                System.out.println("произошла ошибка при обмене данными с удаленным адресом");

            } catch (ClassNotFoundException e) {
                System.out.println("произошла ошибка при обмене данными с удаленным адресом");
            }
            connectionMap.remove(userName);
            sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            System.out.println("соединение с удаленным адресом закрыто");

        }
//реализует протокол общения с клиентом
        private Socket socket;

        Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException { //запрос имени и добавление
            connection.send(new Message(MessageType.NAME_REQUEST, "Введите имя клиента"));       //нового пользователя в мапу
            Message receive = connection.receive();
            if (receive.getType().equals(MessageType.USER_NAME) &&
                    !receive.getData().isEmpty() && !connectionMap.containsKey(receive.getData())) {
                connectionMap.putIfAbsent(receive.getData(), connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED, "имя клиента принято"));
            } else {
                return serverHandshake(connection);
            }
            return receive.getData();
        }

        //отправка клиенту (новому участнику) информации об остальных клиентах (участниках) чата
        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (Map.Entry<String, Connection> map : connectionMap.entrySet()) {
                String name = map.getKey();
                if (!name.equals(userName))
                    connection.send(new Message(MessageType.USER_ADDED, name));
            }
        }

        //главный цикл обработки сообщений сервером
        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {

            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    String mess = userName + ": " + message.getData();
                    Message message1 = new Message(MessageType.TEXT, mess);
                    Server.sendBroadcastMessage(message1);
                } else {
                    ConsoleHelper.writeMessage("Error!");
                }
            }
        }

    }

        public static void sendBroadcastMessage(Message message) { //отправлять сообщение message всем соединениям из connectionMap
            for (String name : connectionMap.keySet()) {
                try {
                    connectionMap.get(name).send(message);
                } catch (IOException e) {
                    ConsoleHelper.writeMessage(String.format("Can't send the message to %s", name));
                }
            }
        }

        public static void main(String[] args) throws IOException {
            /*создаем серверный сокет, считывая порт с консоли
             */

            ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt());
            System.out.println("Server has started");
            try {


                while (true) {
                    Socket socket = serverSocket.accept();
                    Handler handler = new Handler(socket);
                    handler.start();
                }

            } catch (IOException e) {
                System.out.println("ошибка");
                serverSocket.close();
            }
        }
    }

