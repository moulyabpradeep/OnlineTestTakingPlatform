package com.merittrac.apollo.rps.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merittrac.apollo.rps.common.RpsException;

/**
 * Created with IntelliJ IDEA.
 * User: Ajit_K
 * Date: 3/7/14
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */

public class QPPackParser implements Callable {

    protected static final Logger logger = LoggerFactory.getLogger(QPPackParser.class);

    private QPackUploadService qPackUploadService;

    private String qpPackFolder;

    private static final String setCode = "set";

    public QPPackParser(String folder, QPackUploadService qPackUploadService) {
        this.qpPackFolder = folder;
        this.qPackUploadService = qPackUploadService;
    }




    @Override
    public Object call() throws FileNotFoundException, RpsException {
        try {
			qPackUploadService.call(qpPackFolder);
		} catch (IOException e) {
			logger.error("Exception while uploading QPPack from folder " + qpPackFolder, e);
		}
        finally {
        	FileUtils.deleteQuietly(new File(qpPackFolder));
        }
        return null;
    }
}
