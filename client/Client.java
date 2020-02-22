package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;
import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false; //клиент подсоединен к серверу

    public class SocketThread extends Thread {
        @Override
        public void run() {
            super.run();

            try {
                Socket clientSocket = new Socket(getServerAddress(), getServerPort());
                 Connection connection = new Connection(clientSocket);
                 Client.this.connection = connection;
                 clientHandshake();
                 clientMainLoop();
            } catch (IOException e) {
                notifyConnectionStatusChanged(false);
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
                e.printStackTrace();
            }
        }

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(String.format("участник с именем %s присоединился к чату", userName));
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(String.format("участник с именем %s окинул чат.", userName));
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            synchronized (Client.this) {
                Client.this.clientConnected = clientConnected;
                Client.this.notify(); //пробуждаем поток Client, который ожидал подключения к серверу
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException { //проверяем сообщение юзера

            while (true) {
                Message serverResponse = connection.receive();

                if (serverResponse == null || serverResponse.getType() == null) {
                    throw new IOException("Unexpected MessageType");
                }

                MessageType messageType = serverResponse.getType();

                if (messageType == MessageType.NAME_REQUEST) {
                    Message userNameResponse = new Message(MessageType.USER_NAME, getUserName());
                    connection.send(userNameResponse); //отправляем имя юзера серверу
                } else if (messageType == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true); //клиент подключен
                    return;
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException { //главный цикл обработки сообщений сервера

            while (true) {
                Message serverResponse = connection.receive();

                if (serverResponse == null || serverResponse.getType() == null) {
                    throw new IOException("Unexpected MessageType");
                }

                MessageType messageType = serverResponse.getType();

                if (messageType == MessageType.TEXT) {
                    processIncomingMessage(serverResponse.getData());
                } else if (messageType == MessageType.USER_ADDED) {
                    informAboutAddingNewUser(serverResponse.getData());
                } else if (messageType == MessageType.USER_REMOVED) {
                    informAboutDeletingNewUser(serverResponse.getData());
                } else throw new IOException("Unexpected MessageType");
            }
        }
    }

    public static void main(String[] args) {
        new Client().run();
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this) {
            try {
                wait(); //текущий поток ожидает, пока он не получит нотификацию из другого потока.
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("ошибка соединения.");
                System.exit(1);
            }
        }

        if (clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
            String messageUser = "";

            while (clientConnected) {
                messageUser = ConsoleHelper.readString();
                if (messageUser.equals("exit")) break;
                if (shouldSendTextFromConsole()) sendTextMessage(messageUser);
            }

        } else ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
    }

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера: ");
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите адрес порта: ");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Введите имя пользователя: ");
        return ConsoleHelper.readString();
    }
    //всегда должен возвращать true (мы всегда отправляем текст введенный в консоль)
    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }
    //создает новое текстовое сообщение, используя переданный текст и отправляет его серверу через соединение connection
    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            System.out.println("ошибка отправки сообщения!");
            clientConnected = false;
        }
    }

}
