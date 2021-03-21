package com.jn.xingdaba.pay.infrastructure.config;

import com.jn.core.dto.KeyValueDto;
import com.jn.xingdaba.pay.infrastructure.exception.PayException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jn.xingdaba.pay.infrastructure.exception.PaySystemError.GET_OPEN_ID_ERROR;

@Slf4j
public class XmlAssembler {
    /**
     * 组装Xml字符串
     * @param dtoList 待转对象列表
     * @return 转换后xml字符串
     */
    public static String assembleXml(List<KeyValueDto> dtoList) {
        StringBuilder xmlStr = new StringBuilder("<xml>");

        for (KeyValueDto dto : dtoList) {
            if (StringUtils.isBlank(dto.getKey()) || StringUtils.isBlank(dto.getValue()))  {
                continue;
            }

            xmlStr.append("<".concat(dto.getKey()).concat(">"));

            if (isContainChinese(dto.getValue())) {
                xmlStr.append("<![CDATA[");
            }

            xmlStr.append(dto.getValue());

            if (isContainChinese(dto.getValue())) {
                xmlStr.append("]]>");
            }

            xmlStr.append("</".concat(dto.getKey()).concat(">"));
        }

        xmlStr.append("</xml>");
        return xmlStr.toString();
    }

    /**
     * 解析Xml
     * @param xmlStr 待解析xml字符串
     * @return 解析后Map
     */
    public static Map<String, String> analysisXml(String xmlStr) {
        if (StringUtils.isBlank(xmlStr)) {
            return null;
        }

        Map<String, String> retMap = new HashMap<>();

        InputStream in = new ByteArrayInputStream(xmlStr.getBytes());
        SAXBuilder builder = new SAXBuilder();
        Document doc;
        try {
            doc = builder.build(in);
        } catch (JDOMException | IOException e) {
            log.error("analysis xml error.", e);
            throw new PayException(GET_OPEN_ID_ERROR, e.getMessage());
        }

        Element root = doc.getRootElement();
        List<Element> list = root.getChildren();
        for (Element e : list) {
            String key = e.getName();
            String value;
            List<Element> children = e.getChildren();
            if (children.isEmpty()) {
                value = e.getTextNormalize();
            } else {
                value = getChildrenText(children);
            }

            retMap.put(key, value);
        }

        //关闭流
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return retMap;
    }

    private static String getChildrenText(List<Element> children) {
        StringBuilder sb = new StringBuilder();
        if(!children.isEmpty()) {
            for (Element e : children) {
                String name = e.getName();
                String value = e.getTextNormalize();
                List<Element> list = e.getChildren();
                sb.append("<").append(name).append(">");
                if (!list.isEmpty()) {
                    sb.append(getChildrenText(list));
                }
                sb.append(value);
                sb.append("</").append(name).append(">");
            }
        }

        return sb.toString();
    }

    /**
     * 字符串是否包含中文
     *
     * @param str 待校验字符串
     * @return true 包含中文字符  false 不包含中文字符
     */
    private static boolean isContainChinese(String str) {

        if (StringUtils.isBlank(str)) {
            return false;
        }

        Pattern p = Pattern.compile("[\u4E00-\u9FA5|\\！|\\，|\\。|\\（|\\）|\\《|\\》|\\“|\\”|\\？|\\：|\\；|\\【|\\】]");
        Matcher m = p.matcher(str);

        return m.find();
    }
}
