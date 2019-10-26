package com.zufar.service;

import com.zufar.dto.Order;
import com.zufar.dto.Client;
import com.zufar.dto.ClientDTO;
import com.zufar.entity.ClientType;
import com.zufar.entity.ClientEntity;
import com.zufar.exception.InternalServerException;
import com.zufar.repository.ClientRepository;
import com.zufar.exception.ClientNotFoundException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;


@Service
@Transactional
public class ClientService {

    private static final Logger LOGGER = LogManager.getLogger(ClientService.class);

    private final OrderService orderService;
    private final ClientRepository clientRepository;
    private final ClientTypeService clientTypeService;

    @Autowired
    public ClientService(ClientRepository clientRepository,
                         ClientTypeService clientTypeService,
                         OrderService orderService) {
        this.orderService = orderService;
        this.clientRepository = clientRepository;
        this.clientTypeService = clientTypeService;
    }

    public List<Client> getAll() {
        List<ClientEntity> clients;
        try {
            clients = (List<ClientEntity>) clientRepository.findAll();
        } catch (Exception exception) {
            String errorMessage = "It is impossible to get all clients. There are some problems with a database.";
            LOGGER.error(errorMessage, exception);
            throw new InternalServerException(errorMessage, exception);
        }
        LOGGER.info("All clients were loaded from a database successfully.");
        return clients
                .stream()
                .map(this::convertToClient)
                .collect(Collectors.toList());
    }

    public Client getById(Long id) {
        this.isExists(id);
        ClientEntity client;
        try {
            client = clientRepository.findById(id).orElse(null);
        } catch (Exception exception) {
            String errorMessage = String.format("It is impossible to get client with id=[%d]. There are some problems with a database.", id);
            LOGGER.error(errorMessage, exception);
            throw new InternalServerException(errorMessage, exception);
        }
        LOGGER.info(String.format("Client with id=[%d] was loaded from a database successfully.", id));
        return convertToClient(client);
    }

    public Client save(ClientDTO client) {
        ClientEntity clientEntity = convertToClient(client);
        try {
            clientEntity = this.clientRepository.save(clientEntity);
        } catch (Exception exception) {
            String errorMessage = String.format("It is impossible to save a client [%s]. There are some problems with a database.", client);
            LOGGER.error(errorMessage, exception);
            throw new InternalServerException(errorMessage, exception);
        }
        LOGGER.info(String.format("Client=[%s] was saved in a database successfully.", client));
        return convertToClient(clientEntity);
    }

    public Client update(ClientDTO client) {
        this.isExists(client.getId());
        ClientEntity clientEntity = convertToClient(client);
        try {
            clientEntity = this.clientRepository.save(clientEntity);
        } catch (Exception exception) {
            String errorMessage = String.format("It is impossible to update a client [%s]. There are some problems with a database.", client);
            LOGGER.error(errorMessage, exception);
            throw new InternalServerException(errorMessage, exception);
        }
        LOGGER.info(String.format("Client=[%s] was updated in a database successfully.", client));
        return convertToClient(clientEntity);
    }

    public void deleteById(Long id) {
        final ClientEntity client = this.clientRepository.findById(id).orElse(null);
        if (client == null) {
            final String errorMessage = String.format("The client with id=[%d] not found.", id);
            LOGGER.error(errorMessage);
            throw new ClientNotFoundException(errorMessage);
        }
        try {
            this.clientRepository.deleteById(id);
        } catch (Exception exception) {
            String errorMessage = String.format("It is impossible to delete client with id=[%b]. There are some problems with a database.", id);
            LOGGER.error(errorMessage, exception);
            throw new InternalServerException(errorMessage, exception);
        }
        LOGGER.info(String.format("Client with id=[%d] was deleted successfully.", id));
        try {
            this.orderService.deleteOrders(client.getId());
        } catch (Exception exception) {
            String errorMessage = String.format("It is impossible to delete a client [%s]. There are some problems with a database.", client);
            LOGGER.error(errorMessage, exception);
            throw new InternalServerException(errorMessage, exception);
        }
        LOGGER.info(String.format("All orders of client with id=[%d] was deleted successfully.", id));
    }

    private void isExists(Long id) {
        if (!this.clientRepository.existsById(id)) {
            final String errorMessage = "The client with id = " + id + " not found.";
            ClientNotFoundException clientNotFoundException = new ClientNotFoundException(errorMessage);
            LOGGER.error(errorMessage, clientNotFoundException);
            throw clientNotFoundException;
        }
        LOGGER.info(String.format("Client with id=[%d] is existed.", id));
    }

    private Client convertToClient(ClientEntity client) {
        Objects.requireNonNull(client, "There is no a client to convert.");
        List<Order> orders = this.getOrders(client.getOrders());
        final Client result = new Client(
                client.getId(),
                client.getShortName(),
                client.getFullName(),
                client.getClientType(),
                client.getInn(),
                client.getOkpo(),
                orders,
                client.getCreationDate(),
                client.getModificationDate());
        LOGGER.info(String.format("A client entity - [%s] was converted to the client - [%s] successfully.", client, result));
        return result;
    }

    private List<Order> getOrders(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Order> orders;
        try {
            orders = orderService.getOrdersByIds(orderIds).getBody();
        } catch (Exception exception) {
            String errorMessage = String.format("It is impossible to load orders with ids=[%s]. There are some problems with a order service.", orderIds);
            LOGGER.error(errorMessage, exception);
            throw new InternalServerException(errorMessage, exception);
        }
        LOGGER.info(String.format("All orders with ids=[%s] were loaded from a database successfully.", orderIds));
        return orders;
    }

    private ClientEntity convertToClient(ClientDTO client) {
        Objects.requireNonNull(client, "There is no client to convert.");
        ClientType clientType = this.clientTypeService.getById(client.getClientTypeId());
        final ClientEntity result = new ClientEntity(
                client.getId(),
                client.getShortName(),
                client.getFullName(),
                clientType,
                client.getInn(),
                client.getOkpo(),
                client.getOrderIds(),
                client.getCreationDate(),
                client.getModificationDate());
        LOGGER.info(String.format("A client dto - [%s] was converted to the client entity - [%s] successfully.", client, result));
        return result;
    }
}
