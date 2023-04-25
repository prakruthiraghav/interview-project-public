package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.BalanceHistoryRepository;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.BalanceHistoryView;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BalanceHistoryRepository balanceHistoryRepository;

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length

        // Return error if specified user does not exist
        Optional<User> userOptional = userRepository.findById(payload.getUserId());
        if (userOptional.isEmpty()) {
            ResponseEntity<Integer> response = new ResponseEntity<Integer>(HttpStatus.BAD_REQUEST);
            return response;
        }

        // If credit card with same number already exists, return error
        CreditCard creditCardProbe = new CreditCard();
        creditCardProbe.setNumber(payload.getCardNumber());
        Example<CreditCard> creditCardExample = Example.of(creditCardProbe, ExampleMatcher.matchingAny());
        if (creditCardRepository.exists(creditCardExample)) {
            ResponseEntity<Integer> response = new ResponseEntity<Integer>(HttpStatus.BAD_REQUEST);
            return response;
        }

        CreditCard creditCard = new CreditCard();
        creditCard.setUser(userOptional.get());
        creditCard.setNumber(payload.getCardNumber());
        creditCard.setIssuanceBank(payload.getCardIssuanceBank());
        creditCardRepository.save(creditCard);

        ResponseEntity<Integer> response = new ResponseEntity<Integer>(creditCard.getId(), HttpStatus.OK);
        return response;
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null

        // Return error if specified user does not exist
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            ResponseEntity<List<CreditCardView>> response =
                    new ResponseEntity<List<CreditCardView>>(HttpStatus.BAD_REQUEST);
            return response;
        }

        User user = userOptional.get();

        List<CreditCard> creditCardList = user.getCreditCardList();
        List<CreditCardView> creditCardViewList = new ArrayList<CreditCardView>();
        // Map to the view object
        for (CreditCard creditCard : creditCardList) {
            CreditCardView creditCardView = new CreditCardView(creditCard.getIssuanceBank(),
                    creditCard.getNumber());
            creditCardViewList.add(creditCardView);
        }

        ResponseEntity<List<CreditCardView>> response = new ResponseEntity<List<CreditCardView>>(creditCardViewList, HttpStatus.OK);
        return response;
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such credit card exists, return 400 Bad Request

        // Return error if specified credit card does not exist
        CreditCard creditCardProbe = new CreditCard();
        creditCardProbe.setNumber(creditCardNumber);
        Example<CreditCard> creditCardExample = Example.of(creditCardProbe, ExampleMatcher.matchingAny());
        Optional<CreditCard> creditCardOptional = creditCardRepository.findOne(creditCardExample);
        if (creditCardOptional.isEmpty()) {
            ResponseEntity<Integer> response = new ResponseEntity<Integer>(HttpStatus.BAD_REQUEST);
            return response;
        }

        CreditCard creditCard = creditCardOptional.get();
        ResponseEntity<Integer> response = new ResponseEntity<Integer>(creditCard.getUser().getId(), HttpStatus.OK);
        return response;
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Integer> updateBalanceHistoryForCreditCard(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a transaction of {date: 4/10, amount: 10}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 110}]
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.

       // Retrieve specified credit card, return error if it does not exist
       CreditCard creditCardProbe = new CreditCard();
       creditCardProbe.setNumber(payload[0].getCreditCardNumber());
       Example<CreditCard> creditCardExample = Example.of(creditCardProbe, ExampleMatcher.matchingAny());
       Optional<CreditCard> creditCardOptional = creditCardRepository.findOne(creditCardExample);
       if (creditCardOptional.isEmpty()) {
           ResponseEntity<Integer> response = new ResponseEntity<Integer>(HttpStatus.BAD_REQUEST);
           return response;
       }

       CreditCard creditCard = creditCardOptional.get();
       List<BalanceHistory> balanceHistoryList = creditCard.getBalanceHistoryList();

       // Check if balance history for today has to be added
       boolean addBalanceHistoryForToday = false;
       if (balanceHistoryList.isEmpty()) {
           addBalanceHistoryForToday = true;
       } else {
           // The balance history list is sorted in descending order by date.
           Instant latestBalanceHistoryDate = balanceHistoryList.get(0).getDate();
           // If latest balance history is not for today.  Truncate time to only compare with date
           if (latestBalanceHistoryDate.isBefore(Instant.now().truncatedTo(ChronoUnit.DAYS))) {
               addBalanceHistoryForToday = true;
           }
       }
       // Since balance history doest not exist for today, add one to the list with balance as 0.0.
        // Balance amount will get updated when balance history payload is processed
       if (addBalanceHistoryForToday == true) {
           BalanceHistory balanceHistory = new BalanceHistory();
           balanceHistory.setCreditCard(creditCard);
           // Truncate time part
           balanceHistory.setDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
           balanceHistory.setBalance(0.0);
           balanceHistoryList.add(balanceHistory);
       }

       // Now process the specified balance history payload
       for (UpdateBalancePayload anUpdateBalancePayload : payload) {
           // Truncate payload time part so that only date comparison can be done
           Instant payloadDate = anUpdateBalancePayload.getTransactionTime().truncatedTo(ChronoUnit.DAYS);
           double payloadAmount = anUpdateBalancePayload.getTransactionAmount();

           // For each payload entry, loop thro' the existing balance history
           boolean balHistoryExistsForPayLoadDate = false;
           for (BalanceHistory balanceHistory : balanceHistoryList) {
               // Compare pay load date with balance history date
               int compareTxnDate = balanceHistory.getDate().compareTo(payloadDate);
               // Check if balance history exists for this pay load date
               // If no, new balance history entry has to be added to db
               if (compareTxnDate == 0) {
                   balHistoryExistsForPayLoadDate = true;
               }

               // If balance history date is greater than or equal to the
               // payload date, update the balance amount for that history
               if (compareTxnDate >= 0) {
                   double updatedBalance = balanceHistory.getBalance() + payloadAmount;
                   balanceHistory.setBalance(updatedBalance);
               }
           }

           // If balance history did not exist for the pay load date, add one to the list
           if (balHistoryExistsForPayLoadDate == false) {
               BalanceHistory balanceHistory = new BalanceHistory();
               balanceHistory.setCreditCard(creditCard);
               balanceHistory.setDate(payloadDate);
               balanceHistory.setBalance(payloadAmount);
               balanceHistoryList.add(balanceHistory);
           }
       }

       // Save the updated balance history list
       balanceHistoryRepository.saveAll(balanceHistoryList);

       ResponseEntity<Integer> response = new ResponseEntity<Integer>(HttpStatus.OK);
       return response;
    }

    @GetMapping("/credit-card:balance-history")
    public ResponseEntity<List<BalanceHistoryView>> getBalanceHistoryForCard(@RequestParam String creditCardNumber) {
        // Return error if specified credit card does not exist
        CreditCard creditCardProbe = new CreditCard();
        creditCardProbe.setNumber(creditCardNumber);
        Example<CreditCard> creditCardExample = Example.of(creditCardProbe, ExampleMatcher.matchingAny());
        Optional<CreditCard> creditCardOptional = creditCardRepository.findOne(creditCardExample);
        if (creditCardOptional.isEmpty()) {
            ResponseEntity<List<BalanceHistoryView>> response =
                    new ResponseEntity<List<BalanceHistoryView>>(HttpStatus.BAD_REQUEST);
            return response;
        }

        CreditCard creditCard = creditCardOptional.get();
        List<BalanceHistory> balanceHistoryList = creditCard.getBalanceHistoryList();
        List<BalanceHistoryView> balanceHistoryViewList = new ArrayList<BalanceHistoryView>();
        // Map to the view object
        for (BalanceHistory balanceHistory : balanceHistoryList) {
            BalanceHistoryView balanceHistoryView = new BalanceHistoryView(balanceHistory.getCreditCard().getNumber(),
                    balanceHistory.getDate(),
                    balanceHistory.getBalance());
            balanceHistoryViewList.add(balanceHistoryView);
        }

        ResponseEntity<List<BalanceHistoryView>> response =
                new ResponseEntity<List<BalanceHistoryView>>(balanceHistoryViewList, HttpStatus.OK);
        return response;
    }
}
