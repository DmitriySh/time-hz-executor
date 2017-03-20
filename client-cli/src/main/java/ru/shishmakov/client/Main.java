package ru.shishmakov.client;

import com.google.inject.Guice;
import ru.shishmakov.core.Client;
import ru.shishmakov.core.ClientModule;

/**
 * @author Dmitriy Shishmakov on 13.03.17
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        Guice.createInjector(new ClientModule())
                .getInstance(Client.class)
                .startAsync().await();
    }
}
