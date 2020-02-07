/*
 * Copyright (c) ACCA Corp.
 * All Rights Reserved.
 */
package com.rp.Controller;

import com.rp.Service.UserService;
import com.rp.annotation.RpAutowired;
import com.rp.annotation.RpController;
import com.rp.annotation.RpRequestMapping;

/**
 * @author Rui Peng, 2020/2/5
 * @version OPRA v1.0
 **/
@RpController
public class UserController {

    @RpAutowired
    private UserService userService;

    @RpRequestMapping("/sayHello")
    public String sayHello(){
        return userService.sayHello();
    }

}
