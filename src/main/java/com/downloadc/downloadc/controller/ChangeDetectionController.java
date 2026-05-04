package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.ChangeDetectionService;
import com.downloadc.downloadc.config.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Controller for change detect apis
@RestController
@RequestMapping("/api/changes")
public class ChangeDetectionController {

@Autowired private SessionManager sessionManager;
@Autowired private ChangeDetectionService changeService;


// Check snapshot status
@GetMapping("/status")
public ResponseEntity<?> status(){

// If bot loggedin
if(!sessionManager.isLoggedIn())
return ResponseEntity.status(401).body(Map.of("error","Not logged in."));

// return info
return ResponseEntity.ok(Map.of(
"hasSnapshot",changeService.hasSnapshot(),
"lastChecked",changeService.getLastSnapshotTime()
));
}


// take snapshot only
@PostMapping("/snapshot")
public ResponseEntity<?> snapshot(){

// check login
if(!sessionManager.isLoggedIn())
return ResponseEntity.status(401).body(Map.of("error","Not logged in."));

try{

// Take snapshot
changeService.takeSnapshot(sessionManager.getActiveConfig());

return ResponseEntity.ok(Map.of(
"message","snapshot done",
"lastChecked",changeService.getLastSnapshotTime()
));

}
catch(Exception e){
return ResponseEntity.status(500).body(Map.of("error",e.getMessage()));
}
}


// Run difference check
@PostMapping("/check")
public ResponseEntity<?> check(){

// Check login
if(!sessionManager.isLoggedIn())
return ResponseEntity.status(401).body(Map.of("error","Not logged in"));

try{

// Detect changes
ChangeDetectionService.ChangeReport report=
changeService.detectChanges(sessionManager.getActiveConfig());

// return report
return ResponseEntity.ok(report);

}
catch(Exception e){
System.err.println("erorr "+e.getMessage());
return ResponseEntity.status(500).body(Map.of("error",e.getMessage()));
      }
   }
}
