package com.ssg.bidssgket.user.domain.auction.view;

import org.springframework.web.bind.annotation.GetMapping;

public class AuctionViewController {
    @GetMapping("")
    public String mainpageController(){
        return "index";
    }
}