package commands.search;

import commands.Command;
import commands.CommandEnum;
import commands.Utilities;
import errors.FilterChecker;
import filters.Filter;
import filters.FiltersDao;
import filters.UserFiltersDao;
import network.NetworkServiceImpl;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import responses.Response;
import user.User;
import vacancies.Vacancy;

import java.util.*;

public class OtherCommand extends Command {
    private Map<CommandEnum, Filter> commandsToFilters;
    private ResourceBundle constants = localeService.getMessageBundle(user.getCurrentLocale());
    private final List<CommandEnum> filterCommandEnums = new ArrayList<>(Arrays.asList(CommandEnum.PLACEOFWORK, CommandEnum.EXPERIENCE,
            CommandEnum.SALARYFROM, CommandEnum.SALARYTO, CommandEnum.AGE, CommandEnum.CATALOGUES, CommandEnum.FIND));

    public OtherCommand(String s, User user) {
        super(s, user);
        commandsToFilters = new HashMap<>();
        commandsToFilters.put(CommandEnum.CATALOGUES, Filter.CATALOGUES);
        commandsToFilters.put(CommandEnum.PLACEOFWORK, Filter.PLACE_OF_WORK);
        commandsToFilters.put(CommandEnum.AGE, Filter.AGE);
        commandsToFilters.put(CommandEnum.EXPERIENCE, Filter.EXPERIENCE);
        commandsToFilters.put(CommandEnum.SALARYFROM, Filter.SALARY_FROM);
        commandsToFilters.put(CommandEnum.SALARYTO, Filter.SALARY_TO);
    }

    @Override
    public Response execute() {


        if(filterCommandEnums.contains(user.getState()) && user.getState() != CommandEnum.FIND){
            response = getFilterResponse();
        }else if(user.getState() == CommandEnum.FIND){
            response = getShowVacancyResponse();
        }else{
            response.setMessage(constants.getString("unsupported_cmd"));
        }

        return response;
    }

    private Response getShowVacancyResponse(){
        List<CommandEnum> buttons = new ArrayList<>(Arrays.asList(CommandEnum.ADDFAVORITES));
        NetworkServiceImpl networkService = new NetworkServiceImpl();
        List<Vacancy> vacancies = networkService.getVacanciesList(UserFiltersDao.getInstance().getFilters(user.getChatId()));

        response = new Response();

        s = s.replace("/", "").replace("@\\s+", "");
        Vacancy temp = null;

        if(user.getState() == CommandEnum.FIND){
            for (Vacancy vacancy:
                    vacancies) {
                if(vacancy.getId() == Integer.parseInt(s)){
                    temp = vacancy;
                    break;
                }
            }
            if(temp != null){
                user.setVacancy(temp);
                StringBuilder sb = new StringBuilder();
                sb.append(temp.getDescription())
                        .append(temp.getTown())
                        .append(temp.getProfession())
                        .append(temp.getPublicationDate())
                        .append(temp.getId())
                        .append(temp.getAddress())
                        .append(temp.getEducation());
                response.setMessage(sb.toString());
                response.setMarkup(new InlineKeyboardMarkup(Utilities.mapButtonsByTwo(buttons, user.getCurrentLocale())));
            }else{
                response.setMessage("Shit happened in show detailed");
            }
        }
        return response;
    }

    private Response getFilterResponse(){
        FilterChecker filterChecker = new FilterChecker();
        Response response = new Response();

        if (filterChecker.isCorrectInput(s, commandsToFilters.get(user.getState()))) {
            response = setFilter(s, user);
            response.setMessage(constants.getString("filter_header"));
        } else {
            response.setMessage(constants.getString("filter_error") + "\n" + constants.getString("filter_header"));
        }

        response.setMarkup(new InlineKeyboardMarkup(Utilities.mapButtonsByTwo(filterCommandEnums, user.getCurrentLocale())));

        user.setState(CommandEnum.SEARCH);

        return response;
    }

    private Response setFilter(String s, User user) {

        response = new Response();

        FiltersDao filtersDao = UserFiltersDao.getInstance();
        filtersDao.addFilter(user.getChatId(), commandsToFilters.get(user.getState()), s);

        return response;
    }
}
