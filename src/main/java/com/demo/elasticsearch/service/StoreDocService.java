package com.demo.elasticsearch.service;

import com.demo.elasticsearch.bean.StoreBean;
import com.demo.elasticsearch.model.elasticsearch.doc.StoreDoc;
import com.demo.elasticsearch.repository.elasticsearch.query.StoreDocQuery;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreDocService {
    private final StoreDocQuery storeDocQuery;

    public void createStore(StoreBean storeBean) {
        StoreDoc storeDoc = new StoreDoc();
        storeDoc.setId(StringUtils.isBlank(storeBean.getId()) ? UUID.randomUUID().toString() : storeBean.getId());
        storeDoc.setCode(storeBean.getCode());
        storeDoc.setName(storeBean.getName());

        storeDocQuery.createOrUpdateDocument(storeDoc);
    }

    public void updateStore(StoreBean storeBean) {
        StoreDoc documentById = storeDocQuery.getDocumentById(storeBean.getId());
        if (null == documentById) throw new RuntimeException("id " + storeBean.getId() + " not found");

        StoreDoc storeDoc = new StoreDoc();
        storeDoc.setCode(storeBean.getCode());
        storeDoc.setName(storeBean.getName());

        storeDocQuery.createOrUpdateDocument(storeDoc);
    }

    public void updateStoreName(String id, String name) {
        StoreDoc documentById = storeDocQuery.getDocumentById(id);
        if (null == documentById) throw new RuntimeException("id " + id + " not found");

        StoreDoc storeDoc = new StoreDoc();
        storeDoc.setName(name);

        storeDocQuery.updateDocument(id, storeDoc);
    }

    public List<StoreBean> getStoreWithFilter(String code, String name) {
        return storeDocQuery.searchWithFilter(code, name).stream().map(storeDoc -> {
            StoreBean storeBean = new StoreBean();
            storeBean.setId(storeDoc.getId());
            storeBean.setCode(storeDoc.getCode());
            storeBean.setName(storeDoc.getName());
            return storeBean;
        }).toList();
    }
}
