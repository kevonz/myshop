/*
 * Copyright 2005-2015 jshop.com. All rights reserved.
 * File Head

 */
package net.shopxx.dao.impl;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import net.shopxx.Page;
import net.shopxx.Pageable;
import net.shopxx.dao.PointLogDao;
import net.shopxx.entity.Member;
import net.shopxx.entity.PointLog;

import org.springframework.stereotype.Repository;

/**
 * Dao - 积分记录
 * 
 * @author JSHOP Team
 \* @version 3.X
 */
@Repository("pointLogDaoImpl")
public class PointLogDaoImpl extends BaseDaoImpl<PointLog, Long> implements PointLogDao {

	public Page<PointLog> findPage(Member member, Pageable pageable) {
		if (member == null) {
			return Page.emptyPage(pageable);
		}
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<PointLog> criteriaQuery = criteriaBuilder.createQuery(PointLog.class);
		Root<PointLog> root = criteriaQuery.from(PointLog.class);
		criteriaQuery.select(root);
		criteriaQuery.where(criteriaBuilder.equal(root.get("member"), member));
		return super.findPage(criteriaQuery, pageable);
	}

}