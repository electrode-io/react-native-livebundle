#if __has_include(<React/RCTEventEmitter.h>)
#import <React/RCTEventEmitter.h>
#elif __has_include("RCTEventEmitter.h")
#import "RCTEventEmitter.h"
#else
#import "React/RCTEventEmitter.h" // Required when used as a Pod in a Swift project
#endif

#import <React/RCTBridgeModule.h>

@interface LiveBundle : NSObject <RCTBridgeModule>
- (instancetype)initWithstorageUrl:(NSString *)storageUrl
                  storageUrSulffix:(NSString *)storageUrlSuffix;
- (void)launchUI;
@end
