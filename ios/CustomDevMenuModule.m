#import "CustomDevMenuModule.h"
#import "LiveBundle.h"
#import <Foundation/Foundation.h>
#if __has_include(<React/RCTRootView.h>)
#import <React/RCTRootView.h>
#elif __has_include("RCTRootView.h")
#import "RCTRootView.h"
#else
#import "React/RCTRootView.h" // Required when used as a Pod in a Swift project
#endif

@implementation CustomDevMenuModule

@synthesize bridge = _bridge;

#if __has_include(<React/RCTDevMenu.h>)

RCTDevMenuItem *_devMenuItem;

#endif

RCT_EXPORT_MODULE();

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

- (instancetype)init {
    return [super init];
}

- (void)setBridge:(RCTBridge *)bridge {
    _bridge = bridge;
#if __has_include(<React/RCTDevMenu.h>)
    [_bridge.devMenu addItem:self.devMenuItem];
#endif
}

#if __has_include(<React/RCTDevMenu.h>)

- (RCTDevMenuItem *)devMenuItem {
    if (!_devMenuItem) {
        _devMenuItem = [RCTDevMenuItem
            buttonItemWithTitleBlock:^NSString * {
              return @"Live Bundle";
            }
            handler:^{
              NSLog(@"Download LiveBundle");
              RCTRootView *rootView = [[RCTRootView alloc] initWithBridge:self.bridge
                                                               moduleName:@"LiveBundleUI"
                                                        initialProperties:nil];
              UIViewController *vc = [[UIViewController alloc] init];
              vc.view = rootView;
              UINavigationController *rootVC =
                  (UINavigationController *)[[[UIApplication sharedApplication] keyWindow]
                      rootViewController];
              [rootVC pushViewController:vc animated:true];
              [[NSNotificationCenter defaultCenter] postNotificationName:@"Download LiveBundle"
                                                                  object:nil];
            }];
    }
    return _devMenuItem;
}

#endif

@end
