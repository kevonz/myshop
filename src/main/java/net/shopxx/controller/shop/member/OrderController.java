/*
 * Copyright 2005-2015 jshop.com. All rights reserved.
 * File Head

 */
package net.shopxx.controller.shop.member;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import net.shopxx.Message;
import net.shopxx.Pageable;
import net.shopxx.Setting;
import net.shopxx.controller.shop.BaseController;
import net.shopxx.entity.Cart;
import net.shopxx.entity.CartItem;
import net.shopxx.entity.Goods;
import net.shopxx.entity.Member;
import net.shopxx.entity.Order;
import net.shopxx.entity.Product;
import net.shopxx.entity.Receiver;
import net.shopxx.entity.Shipping;
import net.shopxx.service.MemberService;
import net.shopxx.service.OrderService;
import net.shopxx.service.PaymentMethodService;
import net.shopxx.service.ProductService;
import net.shopxx.service.ReceiverService;
import net.shopxx.service.ShippingMethodService;
import net.shopxx.service.ShippingService;
import net.shopxx.util.SystemUtils;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller - 会员中心 - 订单
 * 
 * @author JSHOP Team
 \* @version 3.X
 */
@Controller("shopMemberOrderController")
@RequestMapping("/member/order")
public class OrderController extends BaseController {

	/** 每页记录数 */
	private static final int PAGE_SIZE = 10;

	@Resource(name = "memberServiceImpl")
	private MemberService memberService;
	@Resource(name = "orderServiceImpl")
	private OrderService orderService;
	@Resource(name = "shippingServiceImpl")
	private ShippingService shippingService;

	@Resource(name = "productServiceImpl")
	private ProductService productService;
	
	@Resource(name = "receiverServiceImpl")
	private ReceiverService receiverService;
	
	@Resource(name = "paymentMethodServiceImpl")
	private PaymentMethodService paymentMethodService;
	
	@Resource(name = "shippingMethodServiceImpl")
	private ShippingMethodService shippingMethodService;

	/**
	 * 检查锁定
	 */
	@RequestMapping(value = "/check_lock", method = RequestMethod.POST)
	public @ResponseBody
	Message checkLock(Long id) {
		Order order = orderService.find(id);
		if (order == null) {
			return ERROR_MESSAGE;
		}
		Member member = memberService.getCurrent();
		if (!member.equals(order.getMember())) {
			return ERROR_MESSAGE;
		}
		if (orderService.isLocked(order, member, true)) {
			return Message.warn("shop.member.order.locked");
		}
		return SUCCESS_MESSAGE;
	}

	/**
	 * 物流动态
	 */
	@RequestMapping(value = "/transit_step", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, Object> transitStep(String shippingSn) {
		Map<String, Object> data = new HashMap<String, Object>();
		Shipping shipping = shippingService.findBySn(shippingSn);
		if (shipping == null || shipping.getOrder() == null) {
			data.put("message", ERROR_MESSAGE);
			return data;
		}
		Member member = memberService.getCurrent();
		if (!member.equals(shipping.getOrder().getMember())) {
			data.put("message", ERROR_MESSAGE);
			return data;
		}
		Setting setting = SystemUtils.getSetting();
		if (StringUtils.isEmpty(setting.getKuaidi100Key()) || StringUtils.isEmpty(shipping.getDeliveryCorpCode()) || StringUtils.isEmpty(shipping.getTrackingNo())) {
			data.put("message", ERROR_MESSAGE);
			return data;
		}
		data.put("message", SUCCESS_MESSAGE);
		data.put("transitSteps", shippingService.getTransitSteps(shipping));
		return data;
	}

	/**
	 * 列表
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public String list(Integer pageNumber, ModelMap model) {
		Member member = memberService.getCurrent();
		Pageable pageable = new Pageable(pageNumber, PAGE_SIZE);
		model.addAttribute("page", orderService.findPage(null, null, member, null, null, null, null, null, null, null, pageable));
		return "/shop/${theme}/member/order/list_mdh";
	}

	/**
	 * 查看
	 */
	@RequestMapping(value = "/view", method = RequestMethod.GET)
	public String view(String sn, ModelMap model) {
		System.out.println(" lsu into member/order/view ");
		Order order = orderService.findBySn(sn);
		if (order == null) {
			return ERROR_VIEW;
		}
		Member member = memberService.getCurrent();
		if (!member.equals(order.getMember())) {
			return ERROR_VIEW;
		}
		Setting setting = SystemUtils.getSetting();
		System.out.println(" lsu .. mobile is: " + member.getMobile() + " order sn is: " + order.getSn() );
		model.addAttribute("isKuaidi100Enabled", StringUtils.isNotEmpty(setting.getKuaidi100Key()));
		model.addAttribute("order", order);
		return "/shop/${theme}/member/order/view";
	}

	/**
	 * 取消
	 */
	@RequestMapping(value = "/cancel", method = RequestMethod.POST)
	public @ResponseBody
	Message cancel(String sn) {
		Order order = orderService.findBySn(sn);
		if (order == null) {
			return ERROR_MESSAGE;
		}
		Member member = memberService.getCurrent();
		if (!member.equals(order.getMember())) {
			return ERROR_MESSAGE;
		}
		if (order.hasExpired() || (!Order.Status.pendingPayment.equals(order.getStatus()) && !Order.Status.pendingReview.equals(order.getStatus()))) {
			return ERROR_MESSAGE;
		}
		if (orderService.isLocked(order, member, true)) {
			return Message.warn("shop.member.order.locked");
		}
		orderService.cancel(order);
		return SUCCESS_MESSAGE;
	}

	/**
	 * 收货
	 */
	@RequestMapping(value = "/receive", method = RequestMethod.POST)
	public @ResponseBody
	Message receive(String sn) {
		Order order = orderService.findBySn(sn);
		if (order == null) {
			return ERROR_MESSAGE;
		}
		Member member = memberService.getCurrent();
		if (!member.equals(order.getMember())) {
			return ERROR_MESSAGE;
		}
		if (order.hasExpired() || !Order.Status.shipped.equals(order.getStatus())) {
			return ERROR_MESSAGE;
		}
		if (orderService.isLocked(order, member, true)) {
			return Message.warn("shop.member.order.locked");
		}
		orderService.receive(order, null);
		return SUCCESS_MESSAGE;
	}

	/**
	 * 结算，修改结算商品
	 * 
	 * @param productId
	 * @param quantity
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/checkout", method = RequestMethod.POST)
	public String checkout(Long productId, Integer quantity, ModelMap model) {
		System.out.println(productId+"=hahahh="+quantity);
		
		if (quantity == null || quantity < 1) {
			return ERROR_VIEW;
		}
		Product product = productService.find(productId);
		if (product == null) {
			return ERROR_VIEW;
		}
//		if (!Goods.Type.exchange.equals(product.getType())) {
//			return ERROR_VIEW;
//		}
		if (!product.getIsMarketable()) {
			return ERROR_VIEW;
		}
		if (quantity > product.getAvailableStock()) {
			return ERROR_VIEW;
		}
		Member member = memberService.getCurrent();
		if (member.getPoint() < product.getExchangePoint() * quantity) {
			return ERROR_VIEW;
		}
		System.out.println(product+"="+member);
		
		Set<CartItem> cartItems = new HashSet<CartItem>();
		CartItem cartItem = new CartItem();
		cartItem.setProduct(product);
		cartItem.setQuantity(quantity);
		cartItems.add(cartItem);
		
		Cart cart = new Cart();
		cart.setMember(member);
		cart.setCartItems(cartItems);
		
		Receiver defaultReceiver = receiverService.findDefault(member);
		Order order = orderService.generate(Order.Type.exchange, cart, defaultReceiver, null, null, null, null, null, null);
		model.addAttribute("productId", productId);
		model.addAttribute("quantity", quantity);
		model.addAttribute("order", order);
		model.addAttribute("defaultReceiver", defaultReceiver);
		model.addAttribute("paymentMethods", paymentMethodService.findAll());
		model.addAttribute("shippingMethods", shippingMethodService.findAll());
		
		System.out.println("checkout_mdh");
		
		return "/shop/${theme}/order/checkout_mdh";
	}

}