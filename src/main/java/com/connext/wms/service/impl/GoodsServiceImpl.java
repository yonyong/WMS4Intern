package com.connext.wms.service.impl;

import com.connext.wms.api.dto.GoodsDTO;
import com.connext.wms.dao.GoodsMapper;
import com.connext.wms.entity.Goods;
import com.connext.wms.entity.GoodsExample;
import com.connext.wms.service.GoodsService;
import com.connext.wms.util.Constant;
import com.connext.wms.util.Page;
import com.connext.wms.util.PageSet;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/**
 * @Author: Yogurt7_
 * @Date: 2019/1/8 10:47
 */
@Slf4j
@Service
public class GoodsServiceImpl implements GoodsService {
    @Autowired
    GoodsMapper goodsMapper;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    Constant constant;

    /**
     * 分页查询所有商品并返回到列表
     */
    @Override
    public Page findAll(Integer currPage) {
        List<Goods> list = goodsMapper.selectByPage((currPage - 1) * Page.PAGE_SIZE, Page.PAGE_SIZE);
        GoodsExample example = new GoodsExample();
        return PageSet.setPage(list, currPage, (long) goodsMapper.countByExample(example));
    }

    /**
     * 根据sku获取商品
     *
     * @param sku
     * @return 商品
     */
    @Override
    public Goods getGoodsBySku(String sku) {
        GoodsExample example = new GoodsExample();
        example.createCriteria().andSkuEqualTo(sku);
        return goodsMapper.selectByExample(example).get(0);
    }

    /**
     * 根据商品id获取商品
     *
     * @param id
     * @return Goods
     */
    @Override
    public Goods getGoodsById(Integer id) {
        GoodsExample example = new GoodsExample();
        example.createCriteria().andIdEqualTo(id);
        return goodsMapper.selectByExample(example).get(0);
    }

    /**
     * 根据skulist返回商品list
     *
     * @param skuList
     * @return List<Goods>
     */
    @Override
    public List<Goods> getGoodsBySkuList(List<String> skuList) {
        GoodsExample goodsExample = new GoodsExample();
        goodsExample.createCriteria().andSkuIn(skuList);
        return goodsMapper.selectByExample(goodsExample);
    }

    /**
     * 更新商品名称和价格并且调用同步接口传给OMS
     *
     * @param goods
     */
    @Override
    public String updateGoodsNameAndPrice(Goods goods) {

        // 根据商品id更改商品名称和价格
        if (StringUtils.isNotBlank(goods.getGoodsName())) {
            //if (!goods.getGoodsName().isEmpty() && goods.getGoodsName() != null && !goods.getGoodsName().equals("")) {

            //   -1,表示价格小于0.0
            //   0,表示价格等于0.0
            //   1,表示价格大于0.0

            if (goods.getGoodsPrice() == null || goods.getGoodsPrice().compareTo(BigDecimal.valueOf(0.0)) <= 0) {
                //提示商品价格不能为负数或0
                return "error";//TODO 定义一个枚举

                // -1,表示价格小于1000000.0
                //  0,表示价格等于1000000.0
                //  1,表示价格大于1000000.0
                //upperLimit是价格校验的上限
            } else if (goods.getGoodsPrice().compareTo(BigDecimal.valueOf(constant.upperLimit)) >= 0) {
                //提示商品价格不能超过1000000
                return "sorry";
            } else {
                goodsMapper.updateByPrimaryKeySelective(goods);
                //  调用同步接口传给OMS
                String flag = "success";
                List<GoodsDTO> goodsDTOSList = new ArrayList<>();
                String sku = goodsMapper.selectByPrimaryKey(goods.getId()).getSku();
                goodsDTOSList.add(goodsMapper.selectGoodsDTOBySku(sku));
                // System.out.println(goodsDTOSList.toString());
                try {
                    restTemplate.postForObject(constant.GOODS_UPDATE_URL, goodsDTOSList, String.class);
                } catch (Exception e) {
                    //提示未同步成功
                    flag = "noSyn";
                    log.info("商品信息同步接口调用失败");
                    e.printStackTrace();
                }

                //System.out.println(goods.getGoodsPrice().intValue());
                //System.out.println(goods.getGoodsPrice().compareTo(BigDecimal.valueOf(1000000.0)));
                return flag;
            }
        } else {
            //提示商品名称不能为空
            return "fail";
        }

    }

    /**
     * 根据关键字查询相关的商品信息
     *
     * @param key
     * @param currPage
     * @return Page
     */
    @Override
    public Page selectByExample(String key, Integer currPage) {
        String newKey = "%" + key + "%";
        GoodsExample example = new GoodsExample();
        example.or().andGoodsNameLike(newKey);
        Page page = new Page();
        if (currPage == null) {
            currPage = 1;
        }
        page.setCurrPage(currPage);
        page.setTotalCount((long) goodsMapper.countByExample(example));
        page.init();
        PageHelper.startPage(currPage, Page.PAGE_SIZE);
        page.setData(goodsMapper.selectByExample(example));
        return page;
        //return PageSet.setPage(list, currPage, (long) goodsMapper.countByExample(example));
    }

    /**
     * 同步商品信息
     *
     * @param id
     * @return
     */
    @Override
    public String synchronizeGoodsById(Integer id) {
        List<GoodsDTO> goodsDTOSList = new ArrayList<>();
        String sku = goodsMapper.selectByPrimaryKey(id).getSku();
        goodsDTOSList.add(goodsMapper.selectGoodsDTOBySku(sku));
        // System.out.println(goodsDTOSList.toString());
        String flag = "success";
        try {
            restTemplate.postForObject(constant.GOODS_UPDATE_URL, goodsDTOSList, String.class);
        } catch (Exception e) {
            flag = "error";
            log.info("商品信息同步接口调用未成功");
        }
        //System.out.println(goods.getGoodsPrice().intValue());
        //System.out.println(goods.getGoodsPrice().compareTo(BigDecimal.valueOf(1000000.0)));
        return flag;
    }
}
