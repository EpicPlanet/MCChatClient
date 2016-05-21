/*
 * MCChatClient - a brief idea of what it does
 * Copyright (C) 2016 Final Child
 *
 * This file is part of MCChatClient.
 *
 * MCChatClient is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MCChatClient is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MCChatClient.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.epicpla.mcchatclient;

import jline.console.ConsoleReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.jansi.AnsiConsole;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.event.session.DisconnectedEvent;
import org.spacehq.packetlib.event.session.PacketReceivedEvent;
import org.spacehq.packetlib.event.session.SessionAdapter;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Proxy;

public class MCChatClient {

    public static boolean useJline = true;
    private static final Logger logger = LogManager.getLogger();
    public static ConsoleReader reader;

    public static Client client;

    public static void main(String[] args) {

        try {
            useJline = !"jline.UnsupportedTerminal".equals(System.getProperty("jilne.terminal"));
            if (args.length == 1) {
                System.setProperty("user.language", "en");
                useJline = false;
            }

            if (useJline) {
                AnsiConsole.systemInstall();
            } else {
                System.setProperty(jline.TerminalFactory.JLINE_TERMINAL, jline.UnsupportedTerminal.class.getName());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        if (System.console() == null && System.getProperty("jline.terminal") == null) {
            System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
            useJline = false;
        }

        try {
            reader = new ConsoleReader(System.in, System.out);
            reader.setExpandEvents(false);
        } catch (Throwable e) {
            try {
                System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
                System.setProperty("user.language", "en");
                useJline = false;
                reader = new ConsoleReader(System.in, System.out);
                reader.setExpandEvents(false);
            } catch (IOException ex) {
                logger.warn((String) null, ex);
            }
        }

        String s;
        try {
            while (true) {
                if (useJline) {
                    s = reader.readLine(">", null);
                } else {
                    s = reader.readLine();
                }
                if (s != null && s.trim().length() > 0) {
                    String[] argss = s.split(" ");
                    if (s.equalsIgnoreCase("/!help")) {
                        logger.info("명령어 목록: help, quit, cls, connect");
                    } else if (s.equalsIgnoreCase("/!quit")) {
                        break;
                    } else if (s.equalsIgnoreCase("/!cls")) {
                        reader.clearScreen();
                    } else if (argss[0].equalsIgnoreCase("/!connect")) {
                        if (argss.length != 5) {
                            logger.info("usage: /!connect <ip> <port> <username> <password>");
                            continue;
                        }
                        connect(argss[1], Integer.parseInt(argss[2]), argss[3], argss[4]);
                    } else {
                        if (client != null && client.getSession().isConnected()) client.getSession().send(new ClientChatPacket(s));
                        else logger.info("No session is online!");
                    }
                }
            }
            PrintWriter out = new PrintWriter(reader.getOutput());

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (client != null && client.getSession().isConnected()) client.getSession().disconnect("Disconnected");
        return;
    }

    public static void connect(String ip, int port, String username, String password) {
        if (client != null && client.getSession().isConnected()) client.getSession().disconnect("Disconnected");

        MinecraftProtocol protocol;
        try {
            protocol = new MinecraftProtocol(username, password);
        } catch (RequestException e) {
            e.printStackTrace();
            return;
        }

        client = new Client(ip, port, protocol, new TcpSessionFactory(Proxy.NO_PROXY));
        client.getSession().addListener(new SessionAdapter() {
            @Override
            public void packetReceived(PacketReceivedEvent event) {
                if(event.getPacket() instanceof ServerChatPacket) {
                    Message message = event.<ServerChatPacket>getPacket().getMessage();
                    logger.info(message.getFullText());
                }
            }
            @Override
            public void disconnected(DisconnectedEvent event) {
                logger.info("Disconnected: " + Message.fromString(event.getReason()).getFullText());
                if (event.getCause() != null) {
                    event.getCause().printStackTrace();
                }
            }
        });

        client.getSession().connect();
    }
}
