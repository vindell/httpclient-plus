 package com.github.hiwepy.httpclient.handler.response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import com.github.hiwepy.httpclient.ResponseContent;
import com.github.hiwepy.httpclient.utils.ErrorResponseUtils;


/**
 * 
 * @className	： NestedResponseHandler
 * @description	： http请求响应处理：返回ResponseContent对象
 * @author 		： <a href="https://github.com/hiwepy">hiwepy</a>
 * @date		： 2017年12月3日 下午4:12:31
 * @version 	V1.0
 */
public class NestedResponseHandler extends AbstractResponseHandler<ResponseContent> {
	
	public NestedResponseHandler(String charset) {
		super(null, charset); 
	}
	
	public NestedResponseHandler(HttpClientContext context, String charset) {
		super(context, charset); 
	}

	@Override
    public ResponseContent handleResponse(HttpResponse response) throws ClientProtocolException, IOException {

		// 从response中取出HttpEntity对象
		HttpEntity entity = response.getEntity();
		if (entity == null) {
			throw new ClientProtocolException("Response contains no content");
		}
		
		 
		/**
    	 * 有些情况下，我们希望可以重复读取Http实体的内容。
    	 * 这就需要把Http实体内容缓存在内存或者磁盘上。最简单的方法就是把Http Entity转化成BufferedHttpEntity，
    	 * 这样就把原Http实体的内容缓冲到了内存中。后面我们就可以重复读取BufferedHttpEntity中的内容。
    	 */
		HttpEntity httpEntity = new BufferedHttpEntity(entity);
		/*if (httpEntity.getContentEncoding() != null) {
            if ("gzip".equalsIgnoreCase(httpEntity.getContentEncoding().getValue())) {
                httpEntity = new GzipDecompressingEntity(httpEntity);
            } else if ("deflate".equalsIgnoreCase(httpEntity.getContentEncoding().getValue())) {
                httpEntity = new DeflateDecompressingEntity(httpEntity);
            }
        }*/
		
		//获取响应类型
		ContentType contentType = ContentType.getOrDefault(httpEntity);
		String charset = contentType.getCharset() == null ? getCharset() : contentType.getCharset().name();
		StatusLine statusLine = response.getStatusLine();
		int status = statusLine.getStatusCode();
        if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
            ResponseContent content =  null;
            try {
				// 响应内容
            	content =  getResponseContent(response,statusLine,httpEntity,charset);
            } finally {
                if(httpEntity != null){
                    httpEntity.getContent().close();
                    // 销毁
                    EntityUtils.consume(httpEntity);
                }
            }
            return content;
        } else {
        	return buildErrorResponse(statusLine,httpEntity,charset);
        }
    }
	
	public ResponseContent buildErrorResponse(StatusLine statusLine,HttpEntity entity,String charset) {
		int statusCode = statusLine.getStatusCode();
		ResponseContent content = new ResponseContent();
		content.setStatusCode(statusCode);
		
        Header enHeader = entity.getContentEncoding();
        content.setEncoding(enHeader != null ? enHeader.getValue().toLowerCase() : charset);
        content.setContentType(getResponseContentType(entity));
        content.setContentTypeString(getResponseContentTypeString(entity));
        content.setContentText(ErrorResponseUtils.getStatusErrorJSON(statusLine,""));
		return content;
	}
	
	public ResponseContent getResponseContent(HttpResponse response,StatusLine statusLine,HttpEntity entity,String charset) throws IOException {
		ResponseContent content = new ResponseContent();
    	

		/*//得得响应的下文文
    	ContentType contentType = ContentType.getOrDefault(entity);
        Charset charset2 = contentType.getCharset();
        Readerreader = new InputStreamReader(entity.getContent(), charset);*/ 
        
    	
    	content.setStatusCode(statusLine.getStatusCode());
		
		 
		// 查看entity的各种指标
        System.out.println(entity.getContentType());
        System.out.println(entity.getContentLength());
        System.out.println(ContentType.getOrDefault(entity).getMimeType());
        
        Header enHeader = entity.getContentEncoding();
        if (enHeader != null) {
           content.setEncoding(charset);
        }
        content.setContentType(getResponseContentType(entity));
        content.setContentTypeString(getResponseContentTypeString(entity));
        content.setContentBytes(entity != null ? EntityUtils.toByteArray(entity): null);
        	content.setContentText(entity != null ? EntityUtils.toString(entity,charset) : null);
        
        // 取出服务器返回的数据流
        content.setContent(entity.getContent());
        //解析响应头信息
        Map<String,String> allHeaders = new HashMap<String,String>();
    	for (Header header : response.getAllHeaders()) {
    		allHeaders.put(header.getName(), header.getValue());
		}
    	content.setAllHeaders(allHeaders);
        
        return content;
    }
	
	public String getResponseContentType(HttpEntity entity) {
        Header contenttype = entity.getContentType();
        if (contenttype == null){
            return null;
        }
        String ret = null;
        try {
            HeaderElement[] hes = contenttype.getElements();
            if (hes != null && hes.length > 0) {
                ret = hes[0].getName();
            }
        } catch (Exception e) {
        }
        return ret;
    }
 
	public String getResponseContentTypeString(HttpEntity entity) {
        Header contenttype = entity.getContentType();
        if (contenttype == null){
            return null;
        }
        return contenttype.getValue();
    }
}

 
