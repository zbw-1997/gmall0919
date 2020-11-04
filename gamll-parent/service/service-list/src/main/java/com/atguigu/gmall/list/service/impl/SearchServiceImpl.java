package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Administrator
 * @create 2020-03-23 20:23
 */
@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    //对商品进行操作，关联到es上
    private GoodsRepository goodsRepository;
    @Autowired
    private RedisTemplate redisTemplate;
    //通过他生成dsl语句
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    //上架到es
    @Override
    public void upperGoods(Long skuId) {
        Goods goods = new Goods();
        //平台属性数据
        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
        if(!CollectionUtils.isEmpty(attrList)){
            List<SearchAttr> searchAttrs = attrList.stream().map(baseAttrInfo -> {
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                searchAttr.setAttrValue(attrValueList.get(0).getValueName());
                return searchAttr;
            }).collect(Collectors.toList());
            goods.setAttrs(searchAttrs);
        }
        //查询sku信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if(skuInfo!=null){
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            goods.setId(skuInfo.getId());
            goods.setTitle(skuInfo.getSkuName());
            goods.setCreateTime(new Date());
        }
        // 查询品牌
        BaseTrademark baseTrademark = productFeignClient.getTrademark(skuInfo.getTmId());
        if (baseTrademark != null){
            goods.setTmId(skuInfo.getTmId());
            goods.setTmName(baseTrademark.getTmName());
            goods.setTmLogoUrl(baseTrademark.getLogoUrl());

        }

        // 查询分类
        BaseCategoryView baseCategoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        if (baseCategoryView != null) {
            goods.setCategory1Id(baseCategoryView.getCategory1Id());
            goods.setCategory1Name(baseCategoryView.getCategory1Name());
            goods.setCategory2Id(baseCategoryView.getCategory2Id());
            goods.setCategory2Name(baseCategoryView.getCategory2Name());
            goods.setCategory3Id(baseCategoryView.getCategory3Id());
            goods.setCategory3Name(baseCategoryView.getCategory3Name());
        }


        goodsRepository.save(goods);
    }

    @Override
    public void lowerGoods(Long skuId) {
        goodsRepository.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
        //定义key
        String key ="hotScore";
        //reidis中存储类型
        Double incrementScore = redisTemplate.opsForZSet().incrementScore(key, "sku:" + skuId, 1);
        if(incrementScore%10==0){//每10次热度更新es数据
            Optional<Goods> optional = goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(Math.round(incrementScore));
            goodsRepository.save(goods);

        }

    }

    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {
        //构建dsl语句
        SearchRequest searchRequest = buildQuery(searchParam);
        //执行dsl语句
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //返回结果集
        SearchResponseVo responseVo = parseSearchResult(searchResponse);
//        private Long total;//总记录数
//        private Integer pageSize;//每页显示的内容
//        private Integer pageNo;//当前页面
//        private Long totalPages;
        responseVo.setPageNo(searchParam.getPageNo());
        responseVo.setPageSize(searchParam.getPageSize());
        long totalPages = (responseVo.getTotal()+searchParam.getPageSize()-1)/searchParam.getPageSize();
        responseVo.setTotalPages(totalPages);
        return responseVo;
    }
    //根据dsl语句返回的结果集
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo =new SearchResponseVo();
        /*
        给该对象中的属性赋值
        private List<SearchResponseTmVo> trademarkList;
        //所有商品的顶头显示的筛选属性 平台属性
        private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
        //检索出来的商品信息
        private List<Goods> goodsList = new ArrayList<>();
         */
        //赋值品牌
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        ParsedLongTerms tmIdAGG = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        List<? extends Terms.Bucket> buckets = tmIdAGG.getBuckets();
        if(!CollectionUtils.isEmpty(buckets)){
            List<SearchResponseTmVo> collect = buckets.stream().map(bucket -> {
                SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
                searchResponseTmVo.setTmId(Long.parseLong(bucket.getKeyAsString()));
                Map<String, Aggregation> stringAggregationMap = bucket.getAggregations().asMap();
                ParsedStringTerms tmNameAGG = (ParsedStringTerms) stringAggregationMap.get("tmNameAgg");
                String keyAsString = tmNameAGG.getBuckets().get(0).getKeyAsString();
                searchResponseTmVo.setTmName(keyAsString);
                ParsedStringTerms tmLogoUrlAGG = (ParsedStringTerms) stringAggregationMap.get("tmLogoUrlAgg");
                String url = tmLogoUrlAGG.getBuckets().get(0).getKeyAsString();
                searchResponseTmVo.setTmLogoUrl(url);
                return searchResponseTmVo;
            }).collect(Collectors.toList());
            searchResponseVo.setTrademarkList(collect);
        }
        //赋值平台属性
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        // 获取attrAgg 下的attrIdAgg 聚合数据
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(attrIdAggBuckets)){
            List<SearchResponseAttrVo> collect = attrIdAggBuckets.stream().map(bucket2 -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                searchResponseAttrVo.setAttrId(bucket2.getKeyAsNumber().longValue());
                ParsedStringTerms attrNameAgg = bucket2.getAggregations().get("attrNameAgg");
                // 获取到了平台属性名的集合对象
                List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                // 赋值平台属性名称
                searchResponseAttrVo.setAttrName(nameAggBuckets.get(0).getKeyAsString());
                // 获取平台属性值数据 Aggregation -->ParsedStringTerms
                ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket2).getAggregations().get("attrValueAgg");
                // 获取buckets 中的数据
                List<? extends Terms.Bucket> valueAggBuckets = attrValueAgg.getBuckets();
                // 获取valueAggBuckets 中的所有数据
                List<String> attrValueList = valueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                // 赋值平台属性值
                searchResponseAttrVo.setAttrValueList(attrValueList);
                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            searchResponseVo.setAttrsList(collect);
        }
        //赋值商品信息
        // 获取商品：
        // 声明一个集合对象来存储商品
        List<Goods> goodsList = new ArrayList<>();
        // 先需要获取到hits
        SearchHits hits = searchResponse.getHits();
        SearchHit[] subHits = hits.getHits();
        if (subHits!=null && subHits.length>0){
            // 循环获取里面的数据
            for (SearchHit subHit : subHits) {
                // 将每一个hits中的source 节点的数据 对象转化为Goods
                // subHit.getSourceAsString() 获取 source 中所有数据
                Goods goods = JSONObject.parseObject(subHit.getSourceAsString(), Goods.class);
                // goods 中的商品名称并不是高亮！ 真正的高亮应该在highlight中
                if (subHit.getHighlightFields().get("title")!=null){
                    // 说明有高亮的数据
                    Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                    // 将原来不是高亮的字段替换成高亮数据
                    goods.setTitle(title.toString());
                }
                // 将每个单独的商品添加到集合
                goodsList.add(goods);
            }
        }
        // 赋值商品集合
        searchResponseVo.setGoodsList(goodsList);
        //  private Long total;//总记录数
        searchResponseVo.setTotal(hits.totalHits);
        return searchResponseVo;
    }
    //构建dsl语句
    private SearchRequest buildQuery(SearchParam searchParam) {
        //构建一个查询器
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder();
        //构建bool
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //判断用户是否用关键字搜索
        if(!StringUtils.isEmpty(searchParam.getKeyword())){
            //operator.and是检索全名称，operator.or是分词检索
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);
            boolQuery.must(title);
        }
        //品牌检索
        // trademark=2:华为
        if(!StringUtils.isEmpty(searchParam.getTrademark())){
            String[] split = StringUtils.split(searchParam.getTrademark(), ":");
            if(split!=null && split.length==2){
                boolQuery.filter(QueryBuilders.termsQuery("tmId",split[0]));
            }
        }
        //分类id检索
        if(searchParam.getCategory1Id()!=null){
            boolQuery.filter(QueryBuilders.termQuery("category1Id",searchParam.getCategory1Id()));
        }
        if(searchParam.getCategory2Id()!=null){
            boolQuery.filter(QueryBuilders.termQuery("category2Id",searchParam.getCategory2Id()));
        }
        if(searchParam.getCategory3Id()!=null){
            boolQuery.filter(QueryBuilders.termQuery("category3Id",searchParam.getCategory3Id()));
        }
        //平台属性检索
        //平台属性Id ，平台属性值名称 平台属性名 23:4G:运行内存
        String[] props = searchParam.getProps();
        if(props!=null && props.length>0){
            for (String prop : props) {
                String[] split = StringUtils.split(prop, ":");
                if(split!=null && split.length==3){
                    // 构建嵌套查询：
                    BoolQueryBuilder boolQuery1 = QueryBuilders.boolQuery();
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                    // 构建根据平台属性Id 过滤
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                    // 根据平台属性值
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue",split[1]));
                    // 将subBoolQuery 子查询结果作为一个独立的对象 放入boolQuery
                    boolQuery1.must(QueryBuilders.nestedQuery("attrs",subBoolQuery, ScoreMode.None));

                    // boolQuery查询结果赋值总的boolQueryBuilder
                    boolQuery.filter(boolQuery1);
                }
            }
        }
        searchSourceBuilder.query(boolQuery);
        //分页
        int from = (searchParam.getPageNo()-1)*searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());
        //排序
        // 1:hotScore 2:price 3:
        String order = searchParam.getOrder();
        if(!StringUtils.isEmpty(order)){
            String[] split = StringUtils.split(order, ":");
            if(split!=null && split.length==2){
                String filed =null;
                switch (split[0]){
                    case "1":
                        filed="hotScore";
                        break;
                    case "2":
                        filed="price";
                        break;
                }
                searchSourceBuilder.sort(filed,"asc".equals(split[1])? SortOrder.ASC:SortOrder.DESC);
            }else {
                searchSourceBuilder.sort("hotScore",SortOrder.DESC);
            }
        }
        //高亮
        HighlightBuilder highlightBuilder =new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);
        //聚合
        // 1.   聚合品牌
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));
        // 添加品牌Agg
        searchSourceBuilder.aggregation(termsAggregationBuilder);

        // 2.   聚合平台属性
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));
        // 过滤结果集
        searchSourceBuilder.fetchSource(new String[]{"id","defaultImg","title","price"},null);

        // 查询数据 知道在哪个index,哪个type 中查询
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);

        System.out.println("dsl:"+searchSourceBuilder.toString());
        // 返回查询请求对象
        return searchRequest;
    }
}
