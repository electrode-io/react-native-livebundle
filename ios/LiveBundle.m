#import "LiveBundle.h"
#import "SSZipArchive.h"
#import <React/RCTBridge+Private.h>
#import <React/RCTBundleURLProvider.h>
#import <UIKit/UIKit.h>

#if __has_include(<React/RCTRootView.h>)
#import <React/RCTRootView.h>
#elif __has_include("RCTRootView.h")
#import "RCTRootView.h"
#else
#import "React/RCTRootView.h"   // Required when used as a Pod in a Swift project
#endif

NSString *const sStorageUrlSuffix = @"?sv=2019-12-12&ss=b&srt=co&sp=rx&se=2021-10-08T03:45:35Z&st=2020-10-07T19:45:35Z&spr=https&sig=uGzhJhkPdx%2FdYbuWdR0YSSoRBpHbWpSQsZpCyxJvtPY%3D";
NSString *const sStorageUrl = @"https://testlivebundle.blob.core.windows.net/demo";
NSString *const liveBundle_zip = @"LB-Bundle.zip";
NSString *const liveBundle_js = @"LB-Bundle.js";

@interface LiveBundle ()
@property (nonatomic, strong) UINavigationController *rootVC;
@end

@implementation LiveBundle
static NSString *bundleId = @"";
static NSString *packageId = @"";
static BOOL isBundleInstalled = NO;
static BOOL isSessionStarted = NO;
static NSString *jsPath = @"";
@synthesize bridge = _bridge;

- (instancetype)init {
    if (self = [super init]) {
        _rootVC = (UINavigationController*)[[[UIApplication sharedApplication] keyWindow] rootViewController];
    }
    return self;
}

RCT_EXPORT_MODULE()
- (NSDictionary *)constantsToExport {
         return @{ @"STORAGE_URL" : sStorageUrl,
                   @"STORAGE_URL_SUFFIX" : sStorageUrlSuffix,
                   @"PACKAGE_ID" : packageId,
                   @"BUNDLE_ID": bundleId,
                   @"IS_BUNDLE_INSTALLED": [NSNumber numberWithBool:isBundleInstalled],
                   @"IS_SESSION_STARTED": [NSNumber numberWithBool:isSessionStarted],
              };
}

+ (BOOL)requiresMainQueueSetup
{
  return YES;
}

- (void)setBridge:(RCTBridge *)bridge
{
  _bridge = bridge;
}

RCT_EXPORT_METHOD(getState:(RCTPromiseResolveBlock)resolve reject:(__unused RCTPromiseRejectBlock)reject) {
    NSDictionary *state = @{@"isBundleInstalled": [NSNumber numberWithBool:isBundleInstalled], @"isSessionStarted": [NSNumber numberWithBool:isSessionStarted], @"packageId": packageId, @"bundleId": bundleId};
    resolve(@[state]);
}

RCT_EXPORT_METHOD(launchUI:(NSDictionary *)props resolve:(RCTPromiseResolveBlock)resolve reject:(__unused RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (![jsPath  isEqual: @""]) {
        NSURL *bundleURL = [[NSURL alloc] initFileURLWithPath:jsPath];
        [self->_bridge.parentBridge setBundleURL:bundleURL];
    }
        RCTRootView *rootView = [[RCTRootView alloc] initWithBridge:self->_bridge
                                                         moduleName:@"LiveBundleUI"
                                                  initialProperties:props];
        UIViewController * vc = [[UIViewController alloc] init];
        vc.view = rootView;
        [self->_rootVC pushViewController:vc animated:true];
        resolve(nil);
    });
}

RCT_EXPORT_METHOD(reset:(RCTPromiseResolveBlock)resolve reject:(__unused RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        packageId = @"";
        bundleId = @"";
        isBundleInstalled = NO;
        jsPath = @"";
        NSURL *url;
#if DEBUG
        url = [[RCTBundleURLProvider sharedSettings] jsBundleURLForBundleRoot:@"index" fallbackResource:nil];
#else
        url = [[NSBundle mainBundle] URLForResource:@"livebundle" withExtension:@"js"];
#endif
        [self->_bridge setBundleURL: url];
        [self->_rootVC popViewControllerAnimated:NO];
        [self->_bridge.parentBridge reload];
        resolve(nil);
    });
}

- (void)launchUI {
    __strong RCTBridge *strongBridge = self.bridge;
    strongBridge.bundleURL = [[RCTBundleURLProvider sharedSettings] jsBundleURLForBundleRoot:@"index" fallbackResource:nil];
    RCTRootView *rootView = [[RCTRootView alloc] initWithBridge:strongBridge
                                                     moduleName:@"LiveBundleUI"
                                              initialProperties:nil];
    UIViewController * vc = [[UIViewController alloc] init];
    vc.view = rootView;
    UINavigationController *rootVC = (UINavigationController*)[[[UIApplication sharedApplication] keyWindow] rootViewController];
    [rootVC pushViewController:vc animated:true];
}

- (void)installBundle {
    dispatch_async(dispatch_get_main_queue(), ^{
        [self->_rootVC popViewControllerAnimated:NO];
        if (![jsPath  isEqual: @""]) {
            NSURL *bundleURL = [[NSURL alloc] initFileURLWithPath:jsPath];
            [self->_bridge setBundleURL:bundleURL];
        }
        [self->_bridge.parentBridge reload];
        });
}

RCT_EXPORT_METHOD(downloadBundle:(NSString *)pId bundleId: (NSString *)bId resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    NSString *urlString = [NSString stringWithFormat:@"%@/packages/%@/%@%@", sStorageUrl, pId, bId, sStorageUrlSuffix];
    NSURL *url = [NSURL URLWithString:urlString];
    bundleId = bId;
    packageId = pId;
    dispatch_queue_t queue = dispatch_get_global_queue(0,0);
    dispatch_async(queue, ^{
        NSData *urlData = [NSData dataWithContentsOfURL:url];
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *path = [paths objectAtIndex:0];
        //Save the data
        NSString *zipPath = [path stringByAppendingPathComponent:liveBundle_zip];
        jsPath = [path stringByAppendingPathComponent:liveBundle_js];
        NSString *liveBundlePath = [path stringByAppendingPathComponent:bundleId];
        zipPath = [zipPath stringByStandardizingPath];
        jsPath = [jsPath stringByStandardizingPath];
        NSError *error = nil;
        liveBundlePath = [liveBundlePath stringByStandardizingPath];
        [urlData writeToFile:zipPath atomically:YES];
        [SSZipArchive unzipFileAtPath:zipPath toDestination:path];
        if ([[NSFileManager defaultManager] fileExistsAtPath:jsPath]) {
            [[NSFileManager defaultManager] removeItemAtPath:jsPath error:&error];
        }
        [[NSFileManager defaultManager] moveItemAtPath:liveBundlePath toPath:jsPath error:&error];
        [[NSFileManager defaultManager] removeItemAtPath:zipPath error:&error];
        resolve(nil);
    });
}

RCT_EXPORT_METHOD(installBundle:(RCTPromiseResolveBlock)resolve reject:(__unused RCTPromiseRejectBlock)reject) {
    NSLog(@"installBundle");
    isBundleInstalled = YES;
    [self installBundle];
}
@end
