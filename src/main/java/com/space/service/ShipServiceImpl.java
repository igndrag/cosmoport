package com.space.service;

import com.space.controller.ShipOrder;
import com.space.exception.BadRequestException;
import com.space.exception.EntityNotFoundException;
import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.repository.ShipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;

import static org.hibernate.criterion.Restrictions.and;

@Service
public class ShipServiceImpl implements ShipService {

    private ShipRepository shipRepository;

    final static long MIN_PROD_DATE = new GregorianCalendar(2800, 01, 01).getTimeInMillis();
    final static long MAX_PROD_DATE = new GregorianCalendar(3020, 01, 01).getTimeInMillis();
    final static int MAX_STRING_PAR_LENGTH = 50;
    final static Double MIN_SHIP_SPEED = 0.01D;
    final static Double MAX_SHIP_SPEED = 0.99D;
    final static Integer MIN_CREW_SIZE = 1;
    final static Integer MAX_CREW_SIZE = 9999;
    private static int foundedShipCounter = 0;
    private final static Ship EMPTY_SHIP = new Ship();

    @Autowired
    public void setShipRepository(ShipRepository shipRepository) {
        this.shipRepository = shipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ship> getShips(Optional<String> name,
                               Optional<String> planet,
                               Optional<ShipType> shipType,
                               Optional<Long> after,
                               Optional<Long> before,
                               Optional<Boolean> isUsed,
                               Optional<Double> minSpeed,
                               Optional<Double> maxSpeed,
                               Optional<Integer> minCrewSize,
                               Optional<Integer> maxCrewSize,
                               Optional<Double> minRating,
                               Optional<Double> maxRating,
                               ShipOrder shipOrder,
                               Integer pageNumber,
                               Integer pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(shipOrder.getFieldName()));
        List<Ship> shipList = shipRepository.findAll(Specification.where(CustomerSpecFilters.stringParFilter("name", name))
                .and(CustomerSpecFilters.stringParFilter("planet", planet))
                .and(CustomerSpecFilters.shipTypeFilter("shipType", shipType))
                .and(CustomerSpecFilters.prodDateFilter("prodDate", after, before))
                .and(CustomerSpecFilters.booleanParFilter("isUsed", isUsed))
                .and(CustomerSpecFilters.speedFilter("speed", minSpeed, maxSpeed))
                .and(CustomerSpecFilters.crewSizeFilter("crewSize", minCrewSize, maxCrewSize))
                .and(CustomerSpecFilters.ratingFilter("rating", minRating, maxRating))
                , pageable).getContent();
        foundedShipCounter = shipList.size();
        return shipList;
    }


    @Override
    public int getCount(Optional<String> name,
                        Optional<String> planet,
                        Optional<ShipType> shipType,
                        Optional<Long> after,
                        Optional<Long> before,
                        Optional<Boolean> isUsed,
                        Optional<Double> minSpeed,
                        Optional<Double> maxSpeed,
                        Optional<Integer> minCrewSize,
                        Optional<Integer> maxCrewSize,
                        Optional<Double> minRating,
                        Optional<Double> maxRating) {

        return shipRepository.findAll(Specification.where(CustomerSpecFilters.stringParFilter("name", name))
                        .and(CustomerSpecFilters.stringParFilter("planet", planet))
                        .and(CustomerSpecFilters.shipTypeFilter("shipType", shipType))
                        .and(CustomerSpecFilters.prodDateFilter("prodDate", after, before))
                        .and(CustomerSpecFilters.booleanParFilter("isUsed", isUsed))
                        .and(CustomerSpecFilters.speedFilter("speed", minSpeed, maxSpeed))
                        .and(CustomerSpecFilters.crewSizeFilter("crewSize", minCrewSize, maxCrewSize))
                        .and(CustomerSpecFilters.ratingFilter("rating", minRating, maxRating))).size();

                 // foundedShipCounter;
    }

    @Override
    @Transactional
    public Ship createShip(Ship newShip) {
        checkShip(newShip);
        Double rating = calculateShipRating(newShip.getSpeed(), newShip.getProdDate(), newShip.getUsed());
        newShip.setRating(rating);
        return shipRepository.saveAndFlush(newShip);
    }

    @Override
    @Transactional(readOnly = true)
    public Ship readShip(Long id) {
        Optional<Ship> ship = shipRepository.findById(id);
        if (ship.isPresent())
            return ship.get();
        else throw new EntityNotFoundException("ID: " + id + " not found in database");
    }

    @Override
    @Transactional
    public Ship updateShip(Long id, Ship updatedShip) {

        Ship existingShip = readShip(id);

            if (updatedShip.getName() != null) {
                checkName(updatedShip.getName());
                existingShip.setName(updatedShip.getName());
            }

            if (updatedShip.getPlanet() != null) {
                checkName(updatedShip.getPlanet());
                existingShip.setPlanet(updatedShip.getPlanet());
            }

            if (updatedShip.getCrewSize() != null) {
                checkCrewSize(updatedShip.getCrewSize());
                existingShip.setCrewSize(updatedShip.getCrewSize());
            }

            if (updatedShip.getProdDate() != null) {
                checkProdDate(updatedShip.getProdDate());
                existingShip.setProdDate(updatedShip.getProdDate());
            }

            if (updatedShip.getSpeed() != null) {
                checkSpeed(updatedShip.getSpeed());
                existingShip.setSpeed(updatedShip.getSpeed());
            }

            if (updatedShip.getUsed() != null) {
                existingShip.setUsed(updatedShip.getUsed());
            }

            if (updatedShip.getShipType() != null) {
                existingShip.setShipType(updatedShip.getShipType());
            }

            Double rating = calculateShipRating(existingShip.getSpeed(), existingShip.getProdDate(), existingShip.getUsed());
            existingShip.setRating(rating);
            return shipRepository.saveAndFlush(existingShip);
    }

    @Override
    @Transactional
    public void deleteShip(Long id) {
        readShip(id);
        shipRepository.deleteById(id);
    }



    private Double calculateShipRating(Double shipSpeed, Date prodDate, Boolean isUsed) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(prodDate);
        int prodYear = calendar.get(Calendar.YEAR);
        double k = isUsed ? 0.5D : 1D;
        double rating = (80 * shipSpeed * k) / (3019 - prodYear + 1);
        return Math.round(rating * 100) / 100D;
    }

    private static void checkName(String name) {
        if (name.length() >= MAX_STRING_PAR_LENGTH || name.equals(""))
            throw new BadRequestException("To long string parameter: " + name);
    }

    private static void checkProdDate (Date prodDate) {
        if (MIN_PROD_DATE > prodDate.getTime() || prodDate.getTime() > MAX_PROD_DATE) {
                throw new BadRequestException("Wrong prod date: " + prodDate);
        }
    }

    private static void checkSpeed (Double speed) {
        if (MIN_SHIP_SPEED > speed || speed > MAX_SHIP_SPEED)
            throw new BadRequestException("Wrong speed: " + speed);
    }

    private static void checkCrewSize (Integer crewSize) {
        if (MIN_CREW_SIZE > crewSize || crewSize > MAX_CREW_SIZE)
            throw new BadRequestException("Wrong crew size: " + crewSize);
    }

    private static void checkShip (Ship ship) {
      if (ship.getName() == null
                || ship.getPlanet() == null
                || ship.getShipType() == null
                || ship.getProdDate() == null
                || ship.getSpeed() == null
                || ship.getCrewSize() == null)
        throw new BadRequestException("One of Ship params is null");

        checkName(ship.getName());
        checkName(ship.getPlanet());
        checkProdDate(ship.getProdDate());
        checkSpeed(ship.getSpeed());
        checkCrewSize(ship.getCrewSize());
        if (ship.getUsed() == null)
            ship.setUsed(false);

    }

}