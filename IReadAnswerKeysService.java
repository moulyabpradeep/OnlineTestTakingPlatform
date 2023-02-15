package com.merittrac.apollo.rps.services;

import java.io.IOException;
import java.util.List;

import com.merittrac.apollo.rps.common.RpsException;

public interface IReadAnswerKeysService {
   List<String> readFile(String fileName, String eventCode, String[] assessmentCodes,
			  String actionCode, String password, boolean isRemovalApplicable) throws IOException, RpsException;
   void removeAllKeys(String eventCode, String[] assessmentCodes);
}
