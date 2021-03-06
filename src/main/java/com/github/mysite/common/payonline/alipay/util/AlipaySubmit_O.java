package com.github.mysite.common.payonline.alipay.util;

import com.github.mysite.common.encrypt.MD5Helper;
import com.github.mysite.common.encrypt.AliPayRSAHelper;
import com.github.mysite.common.payonline.alipay.AlipayConfig;
import com.github.mysite.common.payonline.alipay.util.httpclient.AliPayHttpRequest;
import com.github.mysite.common.payonline.alipay.util.httpclient.AliPayHttpResponse;
import com.github.mysite.common.payonline.alipay.util.httpclient.AliPayHttpProtocolHandler;
import com.github.mysite.common.payonline.alipay.util.httpclient.AliPayHttpResultType;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * description:支付宝各接口请求提交类，构造支付宝各接口表单HTML文本，获取远程HTTP数据
 *
 * @author : jy.chen
 * @version : 1.0
 * @since : 2015-11-30 14:44
 */
public class AlipaySubmit_O {
    /**
     * Logger for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger(AlipaySubmit_O.class);


    /**
     * 生成签名结果
     *
     * @param sPara 要签名的数组
     * @return 签名结果字符串
     */
    public static String buildRequestMysign(Map<String, String> sPara) {
        String prestr = AlipayCore_O.createLinkString(sPara); //把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
        String mysign = "";
        if (AlipayConfig.sign_type.equals("MD5")) {
            mysign = MD5Helper.sign(prestr, AlipayConfig.key, AlipayConfig.input_charset);
        }
        if (AlipayConfig.sign_type.equals("0001")) {
            mysign = AliPayRSAHelper.sign(prestr, AlipayConfig.input_charset);
        }
        return mysign;
    }

    /**
     * 生成要请求给支付宝的参数数组
     *
     * @param sParaTemp 请求前的参数数组
     * @return 要请求的参数数组
     */
    private static Map<String, String> buildRequestPara(Map<String, String> sParaTemp) {
        //除去数组中的空值和签名参数
        Map<String, String> sPara = AlipayCore_O.paraFilter(sParaTemp);
        //生成签名结果
        String mysign = buildRequestMysign(sPara);

        //签名结果与签名方式加入请求提交参数组中
        sPara.put("sign", mysign);

        if(!sPara.get("service").equals("alipay.wap.trade.create.direct") && ! sPara.get("service").equals("alipay.wap.auth.authAndExecute")) {
            sPara.put("sign_type", AlipayConfig.sign_type);
        }
        return sPara;
    }

    /**
     * 建立请求，以表单HTML形式构造（默认）
     *
     * @param sParaTemp     请求参数数组
     * @param strMethod     提交方式。两个值可选：post、get
     * @param strButtonName 确认按钮显示文字
     * @return 提交表单HTML文本
     */
    public static String buildRequest(String ALIPAY_GATEWAY_NEW, Map<String, String> sParaTemp, String strMethod, String strButtonName) {
        //待请求参数数组
        Map<String, String> sPara = buildRequestPara(sParaTemp);
        List<String> keys = new ArrayList<String>(sPara.keySet());

        StringBuilder sbHtml = new StringBuilder();

        sbHtml.append("<form id=\"alipaysubmit\" name=\"alipaysubmit\" action=\"").append(ALIPAY_GATEWAY_NEW).append("_input_charset=").append(AlipayConfig.input_charset).append("\" method=\"").append(strMethod).append("\">");

        for (String key : keys) {
            String value = sPara.get(key);

            sbHtml.append("<input type=\"hidden\" name=\"").append(key).append("\" value=\"").append(value).append("\"/>");
        }

        //submit按钮控件请不要含有name属性
        sbHtml.append("<input type=\"submit\" value=\"").append(strButtonName).append("\" style=\"display:none;\"></form>");

        return sbHtml.toString();
    }

    /**
     * 建立请求，以表单HTML形式构造（默认）
     *
     * @param sParaTemp     请求参数数组
     * @param strMethod     提交方式。两个值可选：post、get
     * @param strButtonName 确认按钮显示文字
     * @return 提交表单HTML文本
     */
    public static String mBuildRequest(String ALIPAY_GATEWAY_NEW, Map<String, String> sParaTemp, String strMethod, String strButtonName) {
        //待请求参数数组
        Map<String, String> sPara = buildRequestPara(sParaTemp);
        List<String> keys = new ArrayList<String>(sPara.keySet());

        StringBuilder sbHtml = new StringBuilder();

        sbHtml.append("<form id=\"alipaysubmit\" name=\"alipaysubmit\" action=\"").append(ALIPAY_GATEWAY_NEW).append("\" method=\"").append(strMethod).append("\">");

        for (String key : keys) {
            String value = sPara.get(key);

            sbHtml.append("<input type=\"hidden\" name=\"").append(key).append("\" value=\"").append(value).append("\"/>");
        }

        //submit按钮控件请不要含有name属性
        sbHtml.append("<input type=\"submit\" value=\"").append(strButtonName).append("\" style=\"display:none;\"></form>");
        sbHtml.append("<script>document.forms['alipaysubmit'].submit();</script>");

        return sbHtml.toString();
    }

    /**
     * 建立请求，以表单HTML形式构造，带文件上传功能
     *
     * @param sParaTemp       请求参数数组
     * @param strMethod       提交方式。两个值可选：post、get
     * @param strButtonName   确认按钮显示文字
     * @param strParaFileName 文件上传的参数名
     * @param ALIPAY_GATEWAY_NEW 支付宝网关地址
     * @return 提交表单HTML文本
     */
    public static String buildRequest(String ALIPAY_GATEWAY_NEW, Map<String, String> sParaTemp, String strMethod, String strButtonName, String strParaFileName) {
        //待请求参数数组
        Map<String, String> sPara = buildRequestPara(sParaTemp);
        List<String> keys = new ArrayList<String>(sPara.keySet());

        StringBuilder sbHtml = new StringBuilder();

        sbHtml.append("<form id=\"alipaysubmit\" name=\"alipaysubmit\"  enctype=\"multipart/form-data\" action=\"").append(ALIPAY_GATEWAY_NEW).append("_input_charset=").append(AlipayConfig.input_charset).append("\" method=\"").append(strMethod).append("\">");

        for (String key : keys) {
            String value = sPara.get(key);

            sbHtml.append("<input type=\"hidden\" name=\"").append(key).append("\" value=\"").append(value).append("\"/>");
        }

        sbHtml.append("<input type=\"file\" name=\"").append(strParaFileName).append("\" />");

        //submit按钮控件请不要含有name属性
        sbHtml.append("<input type=\"submit\" value=\"").append(strButtonName).append("\" style=\"display:none;\"></form>");

        return sbHtml.toString();
    }

    /**
     * 建立请求，以模拟远程HTTP的POST请求方式构造并获取支付宝的处理结果
     * 如果接口中没有上传文件参数，那么strParaFileName与strFilePath设置为空值
     * 如：buildRequest("", "",sParaTemp)
     *
     * @param strParaFileName 文件类型的参数名
     * @param strFilePath     文件路径
     * @param sParaTemp       请求参数数组
     * @param ALIPAY_GATEWAY_NEW 支付宝网关地址
     * @return 支付宝处理结果
     * @throws Exception
     */
    public static String buildRequest(String ALIPAY_GATEWAY_NEW, String strParaFileName, String strFilePath, Map<String, String> sParaTemp) throws Exception {
        //待请求参数数组
        Map<String, String> sPara = buildRequestPara(sParaTemp);

        AliPayHttpProtocolHandler aliPayHttpProtocolHandler = AliPayHttpProtocolHandler.getInstance();

        AliPayHttpRequest request = new AliPayHttpRequest(AliPayHttpResultType.BYTES);
        //设置编码集
        request.setCharset(AlipayConfig.input_charset);

        request.setParameters(generatNameValuePair(sPara));
        request.setUrl(ALIPAY_GATEWAY_NEW + "_input_charset=" + AlipayConfig.input_charset);

        AliPayHttpResponse response = aliPayHttpProtocolHandler.execute(request, strParaFileName, strFilePath);
        if (response == null) {
            return null;
        }
        return response.getStringResult();
    }

    /**
     * 建立请求，以模拟远程HTTP的POST请求方式构造并获取支付宝的处理结果
     * 如果接口中没有上传文件参数，那么strParaFileName与strFilePath设置为空值
     * 如：buildRequest("", "",sParaTemp)
     *
     * @param strParaFileName 文件类型的参数名
     * @param strFilePath     文件路径
     * @param sParaTemp       请求参数数组
     * @param ALIPAY_GATEWAY_NEW 支付宝网关地址
     * @return 支付宝处理结果
     * @throws Exception
     */
    public static String mBuildRequest(String ALIPAY_GATEWAY_NEW, String strParaFileName, String strFilePath, Map<String, String> sParaTemp) throws Exception {
        //待请求参数数组
        Map<String, String> sPara = buildRequestPara(sParaTemp);

        AliPayHttpProtocolHandler aliPayHttpProtocolHandler = AliPayHttpProtocolHandler.getInstance();

        AliPayHttpRequest request = new AliPayHttpRequest(AliPayHttpResultType.BYTES);
        //设置编码集
        request.setCharset(AlipayConfig.input_charset);

        request.setParameters(generatNameValuePair(sPara));
        request.setUrl(ALIPAY_GATEWAY_NEW);

        AliPayHttpResponse response = aliPayHttpProtocolHandler.execute(request, strParaFileName, strFilePath);
        if (response == null) {
            return null;
        }

        return response.getStringResult();
    }


    /**
     * MAP类型数组转换成NameValuePair类型
     *
     * @param properties MAP类型数组
     * @return NameValuePair 类型数组
     */
    private static NameValuePair[] generatNameValuePair(Map<String, String> properties) {
        NameValuePair[] nameValuePair = new NameValuePair[properties.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            nameValuePair[i++] = new NameValuePair(entry.getKey(), entry.getValue());
        }

        return nameValuePair;
    }

    /**
     * 解析远程模拟提交后返回的信息，获得token
     *
     * @param text 要解析的字符串
     * @return 解析结果
     * @throws Exception
     */
    public static String getRequestToken(String text) throws Exception {
        String request_token = "";
        //以“&”字符切割字符串
        String[] strSplitText = text.split("&");
        //把切割后的字符串数组变成变量与数值组合的字典数组
        Map<String, String> paraText = new HashMap<String, String>();
        for (String aStrSplitText : strSplitText) {

            //获得第一个=字符的位置
            int nPos = aStrSplitText.indexOf("=");
            //获得字符串长度
            int nLen = aStrSplitText.length();
            //获得变量名
            String strKey = aStrSplitText.substring(0, nPos);
            //获得数值
            String strValue = aStrSplitText.substring(nPos + 1, nLen);
            //放入MAP类中
            paraText.put(strKey, strValue);
        }

        if (paraText.get("res_data") != null) {
            String res_data = paraText.get("res_data");
            //解析加密部分字符串（RSA与MD5区别仅此一句）
            if (AlipayConfig.sign_type.equals("0001")) {
                res_data = AliPayRSAHelper.decrypt(res_data, AlipayConfig.private_key, AlipayConfig.input_charset);
            }

            //token从res_data中解析出来（也就是说res_data中已经包含token的内容）
            Document document = DocumentHelper.parseText(res_data);
            request_token = document.selectSingleNode("//direct_trade_create_res/request_token").getText();
        }
        return request_token;
    }

    /**
     * 用于防钓鱼，调用接口query_timestamp来获取时间戳的处理函数
     * 注意：远程解析XML出错，与服务器是否支持SSL等配置有关
     *
     * @return 时间戳字符串
     * @throws IOException
     * @throws DocumentException
     */
    @SuppressWarnings("unchecked")
    public static String query_timestamp() throws
                                           DocumentException, IOException {

        //构造访问query_timestamp接口的URL串
        String strUrl = AlipayConfig.ALIPAY_GATEWAY_NEW + "service=query_timestamp&partner=" + AlipayConfig.partner + "&_input_charset" + AlipayConfig.input_charset;
        StringBuilder result = new StringBuilder();

        SAXReader reader = new SAXReader();
        Document doc = reader.read(new URL(strUrl).openStream());

        List<Node> nodeList = doc.selectNodes("//alipay/*");

        for (Node node : nodeList) {
            // 截取部分不需要解析的信息
            if (node.getName().equals("is_success") && node.getText().equals("T")) {
                // 判断是否有成功标示
                List<Node> nodeList1 = doc.selectNodes("//response/timestamp/*");
                for (Node node1 : nodeList1) {
                    result.append(node1.getText());
                }
            }
        }

        return result.toString();
    }


    /**
     * 撤销付款方法或者查询单笔交易方法
     *
     * @param tradeNo    支付宝交易号
     * @param outTradeNo 销售单单号
     * @param service 请求服务号  交易关闭为 close_trade 交易查询为 single_trade_query
     */
    public static Map<String, String> cancelOrQueryAlipay(String outTradeNo, String tradeNo,String service) {
        // 把请求参数打包成数组
        Map<String, String> sParaTemp = new HashMap<>();
        sParaTemp.put("service", service);
        sParaTemp.put("partner", AlipayConfig.partner);
        sParaTemp.put("_input_charset", AlipayConfig.input_charset);

        if (StringUtils.equals(service, AlipayConfig.QUERYSINGLE_TRADE_SRVICE)) {
            sParaTemp.put("out_trade_no", tradeNo);
        } else if (StringUtils.equals(service, AlipayConfig.CLOSE_TRADE_SRVICE)) {
            sParaTemp.put("out_order_no", outTradeNo);
        }

        sParaTemp.put("trade_no", tradeNo);
        // 建立请求
        String sHtmlText = "";
        try {
            sHtmlText = AlipaySubmit_O.buildRequest(AlipayConfig.ALIPAY_GATEWAY_NEW, "", "", sParaTemp);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        if (StringUtils.isNotBlank(sHtmlText)) {
            return readXml(sHtmlText);
        } else {
            return null;
        }
    }

    /**
     * 解析xml字符串
     *
     * @param xmlStr 支付宝处理后返回的字符串
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> readXml(String xmlStr) {
        try {
            Document document = DocumentHelper.parseText(xmlStr);
            Map<String, String> responseMap = new HashMap<>();

            List<Node> nodeList = document.selectNodes("//alipay/*");
            for (Node node : nodeList) {
                if (node.getName().equals("is_success")) {
                    responseMap.put(node.getName(), node.getText());

                }
                if (node.getName().equals("error")) {
                    responseMap.put(node.getName(), node.getText());
                }

                if (node.getName().equals("request")) {
                    List<Node> nodeList1 = document.selectNodes("//request/*");
                    for (Node node1 : nodeList1) {
                        responseMap.put(node1.getName(), node1.getText());
                    }
                }
                if (node.getName().equals("response")) {
                    // 判断是否有成功标示
                    //List<Node> nodeList2 = document.selectNodes("//response/payonline/*");
                    List<Node> nodeList2 = document.selectNodes("//response/trade/*");
                    for (Node node2 : nodeList2) {
                        responseMap.put(node2.getName(), node2.getText());
                    }
                }
                if (node.getName().equals("sign")) {
                    responseMap.put(node.getName(), node.getText());
                }
                if (node.getName().equals("sign_type")) {
                    responseMap.put(node.getName(), node.getText());
                }
            }
            LOG.info(xmlStr);
            return responseMap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
