package com.ssg.bidssgket.user.domain.auction.application;

import com.ssg.bidssgket.user.domain.auction.domain.Auction;
import com.ssg.bidssgket.user.domain.auction.domain.dto.AuctionReqDto;
import com.ssg.bidssgket.user.domain.auction.domain.dto.AuctionResponseDto;
import com.ssg.bidssgket.user.domain.auction.domain.repository.AuctionRepository;
import com.ssg.bidssgket.user.domain.member.domain.Member;
import com.ssg.bidssgket.user.domain.member.domain.repository.MemberRepository;
import com.ssg.bidssgket.user.domain.product.domain.Product;
import com.ssg.bidssgket.user.domain.product.domain.SalesStatus;
import com.ssg.bidssgket.user.domain.product.domain.repository.ProductRepository;
import com.ssg.bidssgket.user.domain.product.view.dto.response.ProductResDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    public Product getProductById(Long productNo) {
        return productRepository.findById(productNo).orElseThrow(IllegalAccessError::new);
    }

    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("No member found with the given email"));
    }

    public int getMinBid(Long productNo) {
        List<Auction> auctions = auctionRepository.findByProductNoOrderByMinTenderPriceDesc(productNo);
        if (auctions.isEmpty()) {
            Product product = getProductById(productNo);
            return (int) (product.getAuctionStartPrice() * 1.01);
        } else {
            return (int) (auctions.get(0).getMinTenderPrice() * 1.01);
        }
    }

    public Auction getAuctionByMemberAndProduct(Long memberNo, Long productNo) {
        return auctionRepository.findFirstByMemberAndProductNoOrderByBidNoDesc(memberNo, productNo);
    }

    @Transactional
    public void registerAuction(AuctionReqDto auctionReqDto, String email) {
        Member member = getMemberByEmail(email);
        Product product = getProductById(auctionReqDto.getProductNo());
        Auction auction = Auction.createAuction(auctionReqDto, member, product);
        auctionRepository.save(auction);
    }

    @Transactional
    public void modifyAuction(String email, Long productNo, int maxTenderPrice) {
        Member member = getMemberByEmail(email);
        Auction auction = getAuctionByMemberAndProduct(member.getMemberNo(), productNo);
        auction.updateMaxTenderPrice(maxTenderPrice);
    }

    public int countAuctionsByMemberAndProduct(String email, Long productNo) {
        Member member = getMemberByEmail(email);
        return auctionRepository.countByMemberNoAndProductNo(member.getMemberNo(), productNo);
    }

    @Transactional
    public void deleteAuction(String email, Long productNo) {
        Member member = getMemberByEmail(email);
        Auction auction = getAuctionByMemberAndProduct(member.getMemberNo(), productNo);
        auction.updateTenderDeleted(true);
        auctionRepository.save(auction);
    }

    @Transactional
    public void endAuction(Long productNo) {
        System.out.println("productNo = " + productNo);
        Product product = productRepository.findById(productNo).orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        product.setSalesStatus(SalesStatus.trading);

        List<Auction> auction = auctionRepository.findByProductNoOrderByMinTenderPriceDesc(productNo);
        if (!auction.isEmpty()) {
            Auction firstBid = auction.get(0);
            Auction secondBid = auction.size() > 1 ? auction.get(1) : firstBid;

            firstBid.updateBidSuccess(true);
            product.setBidSuccessPrice(secondBid.getMinTenderPrice());
        }

        productRepository.save(product);
        auctionRepository.saveAll(auction);
    }

    public boolean isAuctionParticipant(Long memberNo, Long productNo) {
        return auctionRepository.countByMemberNoAndProductNo(memberNo, productNo) > 0;
    }

    public boolean isSeller(Long memberNo, Long productNo) {
        return productRepository.existsByMemberAndProductNo(memberNo, productNo) > 0? true: false;
    }

    @Transactional
    public void abandonBid(Long productNo) {
        Product product = productRepository.findById(productNo).orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        product.setSalesStatus(SalesStatus.sale_pause);
        productRepository.save(product);
    }

    public boolean isWinningBidder(Long memberNo, Long productNo) {
        List<Auction> auction = auctionRepository.findByProductNoOrderByMinTenderPriceDesc(productNo);
        if (!auction.isEmpty()) {
            Auction topAuction = auction.get(0);
            return topAuction.getMember().getMemberNo().equals(memberNo) && !topAuction.getTenderDeleted();
        }
        return false;
    }
}

