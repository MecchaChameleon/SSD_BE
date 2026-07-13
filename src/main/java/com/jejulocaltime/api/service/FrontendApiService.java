package com.jejulocaltime.api.service;

import com.jejulocaltime.api.dto.FrontendDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class FrontendApiService {
    private final JdbcTemplate jdbc;

    private static OffsetDateTime time(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class);
    }
    private static Double decimal(ResultSet rs, String column) throws SQLException {
        Number value = (Number) rs.getObject(column);
        return value == null ? null : value.doubleValue();
    }
    private final RowMapper<ProductResponse> productMapper=(rs,n)->new ProductResponse(
            rs.getLong("id"),rs.getLong("seller_profile_id"),rs.getString("name"),rs.getString("business_name"),
            rs.getString("business_type"),rs.getString("category"),rs.getString("environment_type"),
            rs.getInt("total_quantity"),rs.getInt("remaining_quantity"),rs.getInt("original_price"),
            rs.getInt("minimum_price"),rs.getInt("current_price"),
            rs.getInt("original_price")==0?0d:Math.round((1-rs.getDouble("current_price")/rs.getDouble("original_price"))*1000)/10d,
            time(rs,"available_start_at"),time(rs,"reservation_close_at"),rs.getString("address"),
            decimal(rs,"latitude"),decimal(rs,"longitude"),
            time(rs,"reservation_close_at").isBefore(OffsetDateTime.now().plusHours(1)),null,List.of(),rs.getString("status"),
            time(rs,"created_at"),time(rs,"updated_at"),rs.getBoolean("wishlisted"));

    private String productSelect() { return """
        SELECT p.*, sp.business_name,
          EXISTS(SELECT 1 FROM wishlist w WHERE w.product_id=p.id AND w.user_id=?) AS wishlisted
        FROM product p JOIN seller_profile sp ON sp.id=p.seller_profile_id
        """; }

    public PageResponse<ProductResponse> products(Long userId,String query,String businessType,String category,String sort,int page,int size) {
        var sql=new StringBuilder(productSelect()+" WHERE p.status='ACTIVE' AND p.remaining_quantity>0 AND p.reservation_close_at>now() ");
        var args=new ArrayList<Object>(); args.add(userId==null?-1L:userId);
        if(query!=null&&!query.isBlank()){sql.append("AND (lower(p.name) LIKE lower(?) OR lower(sp.business_name) LIKE lower(?)) ");args.add("%"+query+"%");args.add("%"+query+"%");}
        if(businessType!=null){sql.append("AND p.business_type=? ");args.add(businessType);}
        if(category!=null){sql.append("AND p.category=? ");args.add(category);}
        String order=switch(sort==null?"":sort){case "DEADLINE_ASC"->"p.reservation_close_at ASC";case "DISCOUNT_DESC"->"(p.original_price-p.current_price) DESC";case "PRICE_ASC"->"p.current_price ASC";default->"p.created_at DESC";};
        sql.append("ORDER BY ").append(order).append(" LIMIT ? OFFSET ?");args.add(size);args.add(page*size);
        var content=jdbc.query(sql.toString(),productMapper,args.toArray());
        var countSql=new StringBuilder("SELECT count(*) FROM product p JOIN seller_profile sp ON sp.id=p.seller_profile_id WHERE p.status='ACTIVE' AND p.remaining_quantity>0 AND p.reservation_close_at>now() ");
        var countArgs=new ArrayList<Object>();
        if(query!=null&&!query.isBlank()){countSql.append("AND (lower(p.name) LIKE lower(?) OR lower(sp.business_name) LIKE lower(?)) ");countArgs.add("%"+query+"%");countArgs.add("%"+query+"%");}
        if(businessType!=null){countSql.append("AND p.business_type=? ");countArgs.add(businessType);}
        if(category!=null){countSql.append("AND p.category=? ");countArgs.add(category);}
        Long total=jdbc.queryForObject(countSql.toString(),Long.class,countArgs.toArray());
        return new PageResponse<>(content,page,size,total==null?0:total,(int)Math.ceil((total==null?0:total)/(double)size),(page+1)*size>=(total==null?0:total));
    }

    private final RowMapper<MapPinResponse> mapPinMapper=(rs,n)->new MapPinResponse(
            rs.getLong("id"),rs.getString("name"),rs.getString("business_name"),rs.getString("category"),
            rs.getInt("original_price"),rs.getInt("current_price"),
            rs.getInt("original_price")==0?0d:Math.round((1-rs.getDouble("current_price")/rs.getDouble("original_price"))*1000)/10d,
            decimal(rs,"latitude"),decimal(rs,"longitude"),rs.getString("address"),
            time(rs,"reservation_close_at"),time(rs,"reservation_close_at").isBefore(OffsetDateTime.now().plusHours(1)));

    // 지도에 표시할 판매중 상품 핀 목록. 위경도가 없는 상품은 지도에 찍을 수 없으므로 제외한다.
    // swLat/swLng/neLat/neLng(지도 뷰포트 좌하단·우상단 좌표)를 모두 보내면 그 범위 안의 상품만 반환한다.
    public List<MapPinResponse> mapPins(Double swLat,Double swLng,Double neLat,Double neLng){
        var sql=new StringBuilder("""
            SELECT p.id,p.name,sp.business_name,p.category,p.original_price,p.current_price,
                   p.latitude,p.longitude,p.address,p.reservation_close_at
            FROM product p JOIN seller_profile sp ON sp.id=p.seller_profile_id
            WHERE p.status='ACTIVE' AND p.remaining_quantity>0 AND p.reservation_close_at>now()
              AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL
            """);
        var args=new ArrayList<Object>();
        if(swLat!=null&&swLng!=null&&neLat!=null&&neLng!=null){
            sql.append(" AND p.latitude BETWEEN ? AND ? AND p.longitude BETWEEN ? AND ?");
            args.add(swLat);args.add(neLat);args.add(swLng);args.add(neLng);
        }
        return jdbc.query(sql.toString(),mapPinMapper,args.toArray());
    }
    public ProductResponse product(Long userId,Long id){try{return jdbc.queryForObject(productSelect()+" WHERE p.id=?",productMapper,userId==null?-1L:userId,id);}catch(EmptyResultDataAccessException e){throw new ResponseStatusException(NOT_FOUND,"상품을 찾을 수 없습니다.");}}

    @Transactional
    public ReservationResponse purchase(Long userId, PurchaseRequest request){return createReservation(userId,request.productId(),request.quantity(),null,null,true);}
    @Transactional
    public ReservationResponse reserve(Long userId, ReservationCreateRequest request){return createReservation(userId,request.productId(),request.quantity(),request.visitStartAt(),request.visitEndAt(),false);}
    private ReservationResponse createReservation(Long userId,Long productId,Integer quantity,OffsetDateTime start,OffsetDateTime end,boolean immediate){
        if(quantity==null||quantity<1)throw new ResponseStatusException(BAD_REQUEST,"수량은 1개 이상이어야 합니다.");
        var p=jdbc.queryForMap("SELECT id,current_price,remaining_quantity,status,reservation_close_at FROM product WHERE id=? FOR UPDATE",productId);
        if(!"ACTIVE".equals(p.get("status")))throw new ResponseStatusException(CONFLICT,"판매 중인 상품이 아닙니다.");
        if(((Number)p.get("remaining_quantity")).intValue()<quantity)throw new ResponseStatusException(CONFLICT,"판매 가능한 수량이 부족합니다.");
        int unit=((Number)p.get("current_price")).intValue(); String status=immediate?"COMPLETED":"REQUESTED";
        Long id=jdbc.queryForObject("INSERT INTO reservation(product_id,user_id,quantity,unit_price,total_amount,visit_start_at,visit_end_at,status,payment_status,requested_at,completed_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,now(),CASE WHEN ? THEN now() ELSE null END,now()) RETURNING id",Long.class,productId,userId,quantity,unit,unit*quantity,start,end,status,immediate?"PAID":"UNPAID",immediate);
        if(immediate)jdbc.update("UPDATE product SET remaining_quantity=remaining_quantity-?, status=CASE WHEN remaining_quantity-?<=0 THEN 'PAUSED' ELSE status END, updated_at=now() WHERE id=?",quantity,quantity,productId);
        if(!immediate){Long sellerId=jdbc.queryForObject("SELECT sp.user_id FROM product p JOIN seller_profile sp ON sp.id=p.seller_profile_id WHERE p.id=?",Long.class,productId);notify(sellerId,"RESERVATION_REQUESTED","새로운 예약 요청이 들어왔습니다!","상품 예약 요청을 확인해주세요.","RESERVATION",id);}
        return reservation(userId,id,false);
    }
    public PageResponse<ReservationResponse> buyerReservations(Long userId,String status,int page,int size){String where=" WHERE r.user_id=? AND NOT r.hidden_by_buyer "+(status==null?"":"AND r.status=? ");List<Object>a=new ArrayList<>();a.add(userId);if(status!=null)a.add(status);a.add(size);a.add(page*size);var list=jdbc.query(reservationSql()+where+" ORDER BY r.requested_at DESC LIMIT ? OFFSET ?",reservationMapper,a.toArray());Long total=jdbc.queryForObject("SELECT count(*) FROM reservation r"+where,Long.class,status==null?new Object[]{userId}:new Object[]{userId,status});return page(list,page,size,total);}
    public ReservationResponse reservation(Long userId,Long id,boolean seller){String owner=seller?"sp.user_id=?":"r.user_id=?";try{return jdbc.queryForObject(reservationSql()+" WHERE r.id=? AND "+owner,reservationMapper,id,userId);}catch(EmptyResultDataAccessException e){throw new ResponseStatusException(NOT_FOUND,"예약을 찾을 수 없습니다.");}}
    @Transactional public ReservationResponse cancel(Long userId,Long id,String reason){var old=reservation(userId,id,false);if(!List.of("REQUESTED","APPROVED").contains(old.status()))throw new ResponseStatusException(CONFLICT,"취소할 수 없는 예약입니다.");jdbc.update("UPDATE reservation SET status='CANCELED',cancel_reason=?,canceled_at=now(),updated_at=now() WHERE id=?",reason,id);if("APPROVED".equals(old.status()))jdbc.update("UPDATE product SET remaining_quantity=remaining_quantity+?,status='ACTIVE' WHERE id=?",old.quantity(),old.productId());return reservation(userId,id,false);}
    public void hide(Long userId,Long id){jdbc.update("UPDATE reservation SET hidden_by_buyer=true WHERE id=? AND user_id=? AND status IN ('REJECTED','CANCELED','COMPLETED','NO_SHOW')",id,userId);}

    private String reservationSql(){return "SELECT r.*,p.name product_name,sp.business_name,u.nickname buyer_nickname FROM reservation r JOIN product p ON p.id=r.product_id JOIN seller_profile sp ON sp.id=p.seller_profile_id JOIN users u ON u.id=r.user_id";}
    private final RowMapper<ReservationResponse> reservationMapper=(r,n)->new ReservationResponse(r.getLong("id"),r.getLong("product_id"),r.getString("product_name"),r.getString("business_name"),r.getLong("user_id"),r.getString("buyer_nickname"),r.getInt("quantity"),r.getInt("unit_price"),r.getInt("total_amount"),time(r,"visit_start_at"),time(r,"visit_end_at"),r.getString("status"),r.getString("cancel_reason"),time(r,"requested_at"),r.getString("payment_status"));
    private <T> PageResponse<T> page(List<T> list,int page,int size,long total){return new PageResponse<>(list,page,size,total,(int)Math.ceil(total/(double)size),(page+1)*size>=total);}

    public PageResponse<ProductResponse> wishlist(Long userId,int page,int size){var list=jdbc.query(productSelect()+" JOIN wishlist own_w ON own_w.product_id=p.id AND own_w.user_id=? ORDER BY own_w.created_at DESC LIMIT ? OFFSET ?",productMapper,userId,userId,size,page*size);Long total=jdbc.queryForObject("SELECT count(*) FROM wishlist WHERE user_id=?",Long.class,userId);return page(list,page,size,total);}
    public void addWish(Long userId,Long productId){jdbc.update("INSERT INTO wishlist(user_id,product_id) VALUES(?,?) ON CONFLICT DO NOTHING",userId,productId);}
    public void removeWish(Long userId,Long productId){jdbc.update("DELETE FROM wishlist WHERE user_id=? AND product_id=?",userId,productId);}

    private Long sellerProfileId(Long userId){try{return jdbc.queryForObject("SELECT id FROM seller_profile WHERE user_id=?",Long.class,userId);}catch(EmptyResultDataAccessException e){throw new ResponseStatusException(FORBIDDEN,"승인된 판매자 프로필이 필요합니다.");}}
    public PageResponse<ReservationResponse> sellerReservations(Long userId,String status,LocalDate date,int page,int size){Long sp=sellerProfileId(userId);String where=" WHERE p.seller_profile_id=? "+(status==null?"":"AND r.status=? ")+(date==null?"":"AND r.requested_at::date=? ");List<Object>a=new ArrayList<>();a.add(sp);if(status!=null)a.add(status);if(date!=null)a.add(date);a.add(size);a.add(page*size);var list=jdbc.query(reservationSql()+where+" ORDER BY r.requested_at DESC LIMIT ? OFFSET ?",reservationMapper,a.toArray());return page(list,page,size,list.size());}
    @Transactional public ReservationResponse sellerAction(Long userId,Long id,String action,String reason){var old=reservation(userId,id,true);String required=List.of("approve","reject").contains(action)?"REQUESTED":"APPROVED";if(!required.equals(old.status()))throw new ResponseStatusException(CONFLICT,"현재 예약 상태에서는 처리할 수 없습니다.");String next=switch(action){case "approve"->"APPROVED";case "reject"->"REJECTED";case "complete"->"COMPLETED";case "no-show"->"NO_SHOW";default->throw new ResponseStatusException(BAD_REQUEST,"지원하지 않는 처리입니다.");};if("approve".equals(action)){var remain=jdbc.queryForObject("SELECT remaining_quantity FROM product WHERE id=? FOR UPDATE",Integer.class,old.productId());if(remain==null||remain<old.quantity())throw new ResponseStatusException(CONFLICT,"재고가 부족합니다.");jdbc.update("UPDATE product SET remaining_quantity=remaining_quantity-?, status=CASE WHEN remaining_quantity-?<=0 THEN 'PAUSED' ELSE status END, updated_at=now() WHERE id=?",old.quantity(),old.quantity(),old.productId());}jdbc.update("UPDATE reservation SET status=?,cancel_reason=COALESCE(?,cancel_reason),approved_at=CASE WHEN ?='APPROVED' THEN now() ELSE approved_at END,rejected_at=CASE WHEN ?='REJECTED' THEN now() ELSE rejected_at END,completed_at=CASE WHEN ?='COMPLETED' THEN now() ELSE completed_at END,updated_at=now() WHERE id=?",next,reason,next,next,next,id);if("APPROVED".equals(next)||"REJECTED".equals(next))notify(old.buyerId(),"APPROVED".equals(next)?"RESERVATION_APPROVED":"RESERVATION_REJECTED","APPROVED".equals(next)?"예약이 승인되었습니다.":"예약이 거절되었습니다.",reason,"RESERVATION",id);return reservation(userId,id,true);}

    public DashboardResponse dashboard(Long userId,LocalDate date){Long sp=sellerProfileId(userId);long req=count("SELECT count(*) FROM reservation r JOIN product p ON p.id=r.product_id WHERE p.seller_profile_id=? AND r.status='REQUESTED' AND r.requested_at::date=?",sp,date);long app=count("SELECT count(*) FROM reservation r JOIN product p ON p.id=r.product_id WHERE p.seller_profile_id=? AND r.status='APPROVED' AND r.requested_at::date=?",sp,date);long no=count("SELECT count(*) FROM reservation r JOIN product p ON p.id=r.product_id WHERE p.seller_profile_id=? AND r.status='NO_SHOW' AND r.requested_at::date=?",sp,date);long daily=count("SELECT COALESCE(sum(r.unit_price*r.quantity),0) FROM reservation r JOIN product p ON p.id=r.product_id WHERE p.seller_profile_id=? AND r.status='COMPLETED' AND r.completed_at::date=?",sp,date);long products=count("SELECT count(*) FROM product WHERE seller_profile_id=?",sp);return new DashboardResponse(date,new ReservationCounts(req,app,no),daily,daily,products);}
    public SalesReportResponse sales(Long userId,LocalDate start,LocalDate end,String sort){Long sp=sellerProfileId(userId);String order="REVENUE_ASC".equals(sort)?"revenue ASC":"revenue DESC";var items=jdbc.query("SELECT p.id,p.name,sum(r.quantity) quantity,sum(r.unit_price*r.quantity) revenue FROM reservation r JOIN product p ON p.id=r.product_id WHERE p.seller_profile_id=? AND r.status='COMPLETED' AND r.completed_at::date BETWEEN ? AND ? GROUP BY p.id,p.name ORDER BY "+order,(r,n)->new SalesItem(r.getLong(1),r.getString(2),r.getLong(3),r.getLong(4)),sp,start,end);long revenue=items.stream().mapToLong(SalesItem::revenue).sum(),qty=items.stream().mapToLong(SalesItem::quantity).sum();long settlement=count("SELECT COALESCE(sum(r.unit_price*r.quantity),0) FROM reservation r JOIN product p ON p.id=r.product_id WHERE p.seller_profile_id=? AND r.status='COMPLETED' AND r.payment_status='PAID' AND r.completed_at::date BETWEEN ? AND ?",sp,start,end);return new SalesReportResponse(start,end,revenue,settlement,qty,items);}
    private long count(String sql,Object...args){Long v=jdbc.queryForObject(sql,Long.class,args);return v==null?0:v;}

    public void updateUser(Long userId,UserUpdateRequest request){jdbc.update("UPDATE users SET nickname=COALESCE(?,nickname),profile_image_url=COALESCE(?,profile_image_url),updated_at=now() WHERE id=?",request.nickname(),request.profileImageUrl(),userId);}
    @Transactional public ProductResponse applyPrice(Long userId,Long productId,PriceApplyRequest request){
        sellerProfileId(userId);
        if(request.price()==null||request.price()<1)throw new ResponseStatusException(BAD_REQUEST,"적용 가격은 1원 이상이어야 합니다.");
        // 본인 상품의 최소/최고금액을 먼저 조회(소유권 확인 겸)한 뒤, 적용 가격이 그 범위 안인지 검증한다.
        Map<String,Object> price;
        try{price=jdbc.queryForMap("SELECT p.minimum_price,p.original_price FROM product p JOIN seller_profile sp ON sp.id=p.seller_profile_id WHERE sp.user_id=? AND p.id=?",userId,productId);}
        catch(EmptyResultDataAccessException e){throw new ResponseStatusException(NOT_FOUND,"상품을 찾을 수 없습니다.");}
        int min=((Number)price.get("minimum_price")).intValue(),max=((Number)price.get("original_price")).intValue();
        if(request.price()<min)throw new ResponseStatusException(BAD_REQUEST,"적용 가격("+request.price()+")은 최소금액("+min+")보다 낮을 수 없습니다.");
        if(request.price()>max)throw new ResponseStatusException(BAD_REQUEST,"적용 가격("+request.price()+")은 최고금액("+max+")보다 높을 수 없습니다.");
        jdbc.update("UPDATE product SET current_price=?,updated_at=now() WHERE id=?",request.price(),productId);
        return product(userId,productId);
    }

    public PageResponse<NotificationResponse> notifications(Long userId,String filter,int page,int size){String unread="UNREAD".equals(filter)?" AND NOT is_read":"";var list=jdbc.query("SELECT * FROM notification WHERE user_id=?"+unread+" ORDER BY created_at DESC LIMIT ? OFFSET ?",(r,n)->new NotificationResponse(r.getLong("id"),r.getString("type"),r.getString("title"),r.getString("message"),r.getString("reference_type"),r.getObject("reference_id",Long.class),r.getBoolean("is_read"),time(r,"created_at")),userId,size,page*size);long total=count("SELECT count(*) FROM notification WHERE user_id=?"+unread,userId);return page(list,page,size,total);}
    public void readNotification(Long userId,Long id){jdbc.update("UPDATE notification SET is_read=true,read_at=now() WHERE id=? AND user_id=?",id,userId);}
    public void readAll(Long userId){jdbc.update("UPDATE notification SET is_read=true,read_at=COALESCE(read_at,now()) WHERE user_id=?",userId);}
    public NotificationSettings settings(Long userId){jdbc.update("INSERT INTO notification_setting(user_id) VALUES(?) ON CONFLICT DO NOTHING",userId);return jdbc.queryForObject("SELECT * FROM notification_setting WHERE user_id=?",(r,n)->new NotificationSettings(r.getBoolean("common_event"),r.getBoolean("seller_reservation"),r.getBoolean("seller_ai_price"),r.getBoolean("seller_settlement"),r.getBoolean("buyer_deadline"),r.getBoolean("buyer_reservation_approved")),userId);}
    public NotificationSettings updateSettings(Long userId,NotificationSettings s){jdbc.update("INSERT INTO notification_setting(user_id,common_event,seller_reservation,seller_ai_price,seller_settlement,buyer_deadline,buyer_reservation_approved) VALUES(?,?,?,?,?,?,?) ON CONFLICT(user_id) DO UPDATE SET common_event=EXCLUDED.common_event,seller_reservation=EXCLUDED.seller_reservation,seller_ai_price=EXCLUDED.seller_ai_price,seller_settlement=EXCLUDED.seller_settlement,buyer_deadline=EXCLUDED.buyer_deadline,buyer_reservation_approved=EXCLUDED.buyer_reservation_approved,updated_at=now()",userId,s.commonEvent(),s.sellerReservation(),s.sellerAiPrice(),s.sellerSettlement(),s.buyerDeadline(),s.buyerReservationApproved());return settings(userId);}
    public void pushToken(Long userId,PushTokenRequest r){jdbc.update("INSERT INTO push_token(user_id,device_token,platform) VALUES(?,?,?) ON CONFLICT(device_token) DO UPDATE SET user_id=EXCLUDED.user_id,platform=EXCLUDED.platform,updated_at=now()",userId,r.deviceToken(),r.platform());}
    public void removePushToken(Long userId,String token){jdbc.update("DELETE FROM push_token WHERE user_id=? AND device_token=?",userId,token);}
    private void notify(Long userId,String type,String title,String message,String referenceType,Long referenceId){if(userId!=null)jdbc.update("INSERT INTO notification(user_id,type,title,message,reference_type,reference_id,is_read,created_at) VALUES(?,?,?,?,?,?,false,now())",userId,type,title,message,referenceType,referenceId);}
}
