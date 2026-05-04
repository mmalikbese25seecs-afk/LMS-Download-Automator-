package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.SchedulerService;
import com.downloadc.downloadc.config.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

// Scheduler controller
@RestController
@RequestMapping("/api/scheduler")
  
public class SchedulerController {

@Autowired
  private SessionManager sessionManager;
@Autowired 
  private SchedulerService schedulerService;


// Get current config
@GetMapping("/config")
public ResponseEntity<?> getConfig(){

// Check login
if(!sessionManager.isLoggedIn())
return ResponseEntity.status(401).body(Map.of("error","Not logged in"));

// return saved config
return ResponseEntity.ok(schedulerService.loadConfig());
}


// Save config
@PostMapping("/config")
public ResponseEntity<?> saveConfig(@RequestBody SchedulerService.ScheduleConfig cfg){

// check login
if(!sessionManager.isLoggedIn())
return ResponseEntity.status(401).body(Map.of("error","Not logged in"));

// mode check
if(!List.of("manual","daily","weekly").contains(cfg.mode))
return ResponseEntity.badRequest().body(Map.of("error","bad mode"));

// hour check
if(cfg.hourOfDay<0 || cfg.hourOfDay>23)
return ResponseEntity.badRequest().body(Map.of("error","bad hour"));

try{

// save cfg
schedulerService.saveConfig(cfg);

return ResponseEntity.ok(Map.of(
"msg","saved",
"config",cfg
));

}catch(Exception e){
return ResponseEntity.status(500).body(Map.of("error",e.getMessage()));
}
}


// Run sync now
@PostMapping("/run-now")
public ResponseEntity<?> runNow(){

// check login
if(!sessionManager.isLoggedIn())
return ResponseEntity.status(401).body(Map.of("error","Not logged in"));

SchedulerService.SyncResult result=schedulerService.triggerNow();

// success or fail
if(result.success())
return ResponseEntity.ok(result);

return ResponseEntity.status(500).body(result);
  }
}
